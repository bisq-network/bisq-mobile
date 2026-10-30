package network.bisq.mobile.client.common.domain.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.path
import io.ktor.util.collections.ConcurrentMap
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.bisq.mobile.client.common.domain.access.utils.HeaderRedaction
import network.bisq.mobile.client.common.domain.access.utils.Headers
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.client.common.domain.websocket.exception.MaximumRetryReachedException
import network.bisq.mobile.client.common.domain.websocket.exception.WebSocketIsReconnecting
import network.bisq.mobile.client.common.domain.websocket.exception.WebSocketSessionClosedByServer
import network.bisq.mobile.client.common.domain.websocket.exception.WebSocketSessionClosedEarly
import network.bisq.mobile.client.common.domain.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.common.domain.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.data.replicated.settings.ApiVersionSettingsVO
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.SemanticVersion
import network.bisq.mobile.domain.utils.awaitOrCancel
import network.bisq.mobile.domain.utils.createUuid
import network.bisq.mobile.domain.utils.redactedSummary
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class WebSocketClientImpl(
    private val httpClient: HttpClient,
    private val json: Json,
    override val apiUrl: Url,
    override val sessionId: String?,
    override val clientId: String?,
    private val clientScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : WebSocketClient,
    Logging {
    companion object {
        const val DELAY_TO_RECONNECT = 2000L
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val MAX_RECONNECT_DELAY = 10000L // 10 seconds max delay
        const val RECONNECT_CONNECT_TIMEOUT = 30_000L // shorter timeout during reconnect
        const val HEALTH_CHECK_PATH = "/api/v1/settings/version"
        const val INFO_PATH = "/api/v1/info"

        // Handshake and health check: routine traffic, kept out of the logs.
        private val QUIET_PATHS = setOf(HEALTH_CHECK_PATH, INFO_PATH)
        const val STALE_RECONNECT_THRESHOLD_MS = 30_000L // cancel reconnect if stuck this long
    }

    private var reconnectAttempts = 0
    private var isReconnecting = atomic(false)

    @Volatile
    private var reconnectConnectStartMs = 0L
    private var reconnectJob: Job? = null
    private var session: DefaultClientWebSocketSession? = null
    private val webSocketEventObservers =
        ConcurrentMap<String, WebSocketEventObserver>()
    private val requestResponseHandlers =
        mutableMapOf<String, RequestResponseHandler>()
    private val quietRequestIds = mutableSetOf<String>()
    private val connectionMutex = Mutex()
    private val requestResponseHandlersMutex = Mutex()

    // signal to cancel waiters
    private val stopFlow =
        MutableSharedFlow<Unit>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val _webSocketClientStatus =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    override val webSocketClientStatus: StateFlow<ConnectionState> = _webSocketClientStatus.asStateFlow()

    private val _nodeInfo = MutableStateFlow<ApiInfoDto?>(null)
    override val nodeInfo: StateFlow<ApiInfoDto?> = _nodeInfo.asStateFlow()

    // Remembered per instance so reconnects to a node without /info skip the probe (it is one more
    // Tor round-trip inside the connect budget). A new instance is created whenever settings change.
    private var infoEndpointAbsent = false

    private var listenerJob: Job? = null

    override fun isDemo(): Boolean = false

    override suspend fun connect(timeout: Long): Throwable? {
        connectionMutex.withLock {
            try {
                // websocket wont attempt a connect while connecting due to connectionMutex lock
                // so we always reach here either in connected or disconnected state
                if (isConnected()) {
                    return null
                }
                // A version mismatch won't fix itself; only an explicit disconnect() allows a new handshake
                _webSocketClientStatus.value.incompatibleApiVersionError?.let { return it }
                doDisconnect() // clean up state
                log.d { "WS connecting to $apiUrl (timeout=${timeout}ms) ..." }
                _webSocketClientStatus.value = ConnectionState.Connecting
                val startTime = DateUtils.now()
                val newSession = openSession(timeout.milliseconds)
                val elapsed = DateUtils.now() - startTime
                val remainingTime = timeout - elapsed
                session = newSession
                if (newSession.isActive) {
                    log.i { "WS connected successfully (TCP/upgrade in ${elapsed}ms, remaining=${remainingTime}ms)" }
                    val listener = clientScope.launch { startListening(newSession) }
                    listenerJob = listener

                    withTimeout(remainingTime.milliseconds) { performHandshake() }

                    // The session can close after the handshake response arrived. Its listener then sets
                    // Disconnected, which Connected must not overwrite.
                    val stillOpen = newSession.isActive && listener.isActive && session === newSession
                    if (!stillOpen || !_webSocketClientStatus.compareAndSet(ConnectionState.Connecting, ConnectionState.Connected)) {
                        // The handshake set it after the listener had cleared it
                        _nodeInfo.value = null
                        throw WebSocketSessionClosedEarly()
                    }

                    // Reset reconnect attempts on successful connection
                    reconnectAttempts = 0
                } else {
                    throw WebSocketSessionClosedEarly()
                }
            } catch (e: Throwable) {
                log.e(e) { "WS connection failed to connect" }
                _webSocketClientStatus.value = ConnectionState.Disconnected(e)
                currentCoroutineContext().ensureActive()
                return e
            }
            return null
        }
    }

    private suspend fun openSession(timeout: Duration): DefaultClientWebSocketSession {
        val startTime = DateUtils.now()
        val wsProtocol =
            if (apiUrl.protocol == URLProtocol.HTTPS) URLProtocol.WSS else URLProtocol.WS
        val wsHost = apiUrl.host
        val wsPort = apiUrl.port
        return try {
            withTimeout(timeout) {
                httpClient.webSocketSession {
                    url {
                        // apiUrl.protocol is guaranteed to be HTTP or HTTPS due to upstream validation
                        protocol = wsProtocol
                        host = wsHost
                        port = wsPort
                        path("/websocket")
                    }
                    // Send session credentials on upgrade for servers with password auth
                    sessionId?.let { headers.append(Headers.SESSION_ID, it) }
                    clientId?.let { headers.append(Headers.CLIENT_ID, it) }
                }
            }
        } catch (e: Throwable) {
            val elapsed = DateUtils.now() - startTime
            log.e(e) { "WS TCP/upgrade phase failed after ${elapsed}ms: ${e::class.simpleName}" }
            throw e
        }
    }

    private suspend fun performHandshake() {
        val info = fetchNodeInfo()
        if (!isApiCompatible(info.version)) {
            doDisconnect()
            awaitDisconnection() // so that we update the disconnect reason correctly
            throw IncompatibleHttpApiVersionException(info.version)
        }
        _nodeInfo.value = info
    }

    override suspend fun disconnect() {
        connectionMutex.withLock {
            log.d { "WS disconnect called" }
            doDisconnect()
            if (_webSocketClientStatus.value.incompatibleApiVersionError != null) {
                _webSocketClientStatus.value = ConnectionState.Disconnected()
            }
        }
    }

    /**
     * Disconnect and cleanup logic, without the lock for internal calls
     */
    private suspend fun doDisconnect() {
        // order is important. we want to know the difference between server close and our close
        // so we cancel the listen job so it throws cancellation exception first then cose the session.
        // Joined, so its cleanup can't reset the status or the request handlers of the next connect.
        listenerJob?.cancelAndJoin()
        listenerJob = null
        session?.close()
        session = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun reconnect() {
        // Same rule as connect(): only an explicit disconnect() allows a new handshake
        if (_webSocketClientStatus.value.incompatibleApiVersionError != null) {
            log.d { "Skipping reconnect: node API version is incompatible" }
            return
        }
        if (isReconnecting.getAndSet(true)) {
            // If the connect phase has been running longer than the stale threshold,
            // cancel it so the next triggerReconnect() call can start fresh.
            val connectStart = reconnectConnectStartMs
            val elapsed = if (connectStart > 0) DateUtils.now() - connectStart else 0
            if (elapsed > STALE_RECONNECT_THRESHOLD_MS) {
                log.d { "Reconnect connect stuck for ${elapsed}ms, cancelling for fresh retry" }
                reconnectJob?.cancel()
            }
            // else: reconnect already in progress, silently skip
            return
        }
        reconnectJob?.cancel()
        // A caller that saw Connecting did not see a stale Connected, so the outcome of that connect stands
        val connectInFlight = _webSocketClientStatus.value is ConnectionState.Connecting

        val newReconnectJob: Deferred<Throwable?> =
            clientScope.async {
                log.d { "Launching reconnect attempt #${reconnectAttempts + 1}" }
                // Under the lock, so a connect in flight finishes instead of losing its session mid-handshake
                val keptInFlightConnection =
                    connectionMutex.withLock {
                        if (connectInFlight && isConnected()) {
                            true
                        } else {
                            doDisconnect() // clean up state
                            false
                        }
                    }
                if (keptInFlightConnection) {
                    log.d { "Skipping reconnect: the connect in flight succeeded" }
                    return@async null
                }

                // Check if we've exceeded max attempts
                if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    val e = MaximumRetryReachedException(MAX_RECONNECT_ATTEMPTS)
                    log.w { "Giving up on reconnecting: ${e.redactedSummary()}" }
                    _webSocketClientStatus.value =
                        ConnectionState.Disconnected(e)
                    // Reset counter for future reconnects
                    reconnectAttempts = 0
                    return@async null
                }

                // Implement exponential backoff
                val delayMillis =
                    minOf(
                        DELAY_TO_RECONNECT * (
                            1 shl
                                minOf(
                                    reconnectAttempts,
                                    4,
                                )
                        ), // Exponential backoff
                        MAX_RECONNECT_DELAY,
                    )
                log.d { "Waiting ${delayMillis}ms before reconnect attempt" }
                delay(delayMillis)
                reconnectAttempts++
                // Use shorter timeout during reconnect to avoid long hangs
                val timeout =
                    minOf(
                        WebSocketClient.determineTimeout(apiUrl.host),
                        RECONNECT_CONNECT_TIMEOUT,
                    )
                reconnectConnectStartMs = DateUtils.now()
                val error = connect(timeout)
                reconnectConnectStartMs = 0
                return@async error
            }
        reconnectJob = newReconnectJob
        newReconnectJob.invokeOnCompletion {
            reconnectConnectStartMs = 0
            isReconnecting.value = false
            if (it == null) {
                val connectError = newReconnectJob.getCompleted()
                if (connectError != null && isRetryableError(connectError)) {
                    reconnect()
                }
            }
        }
    }

    private fun isRetryableError(error: Throwable) = error !is UnauthorizedApiAccessException && error !is IncompatibleHttpApiVersionException

    // Blocking request until we get the associated response
    override suspend fun sendRequestAndAwaitResponse(
        webSocketRequest: WebSocketRequest,
        awaitConnection: Boolean,
    ): WebSocketResponse? {
        if (awaitConnection) {
            val timeout = WebSocketClient.determineTimeout(apiUrl.host)
            withTimeout(timeout) {
                awaitConnection()
            }
        }

        val requestId = webSocketRequest.requestId
        val isQuiet = webSocketRequest is WebSocketRestApiRequest && webSocketRequest.path in QUIET_PATHS
        val requestResponseHandler = RequestResponseHandler(this::send, quiet = isQuiet)
        requestResponseHandlersMutex.withLock {
            requestResponseHandlers[requestId] = requestResponseHandler
        }

        try {
            // No session or client headers here: the node takes the identity of a proxied REST call
            // from the authenticated upgrade request and drops any headers the message carries.
            // The version check is the exception, see getRest.
            return requestResponseHandler.request(webSocketRequest)
        } finally {
            requestResponseHandlersMutex.withLock {
                requestResponseHandlers.remove(requestId)
            }
        }
    }

    private suspend fun awaitConnection() {
        awaitOrCancel(
            webSocketClientStatus.filter { it is ConnectionState.Connected },
            stopFlow,
        )
    }

    private suspend fun awaitDisconnection() {
        awaitOrCancel(
            webSocketClientStatus.filter { it is ConnectionState.Disconnected },
            stopFlow,
        )
    }

    override suspend fun subscribe(
        topic: Topic,
        parameter: String?,
        webSocketEventObserver: WebSocketEventObserver,
    ): WebSocketEventObserver {
        val subscriberId = createUuid()
        log.i { "Subscribe for topic $topic and subscriberId $subscriberId" }

        val response: WebSocketResponse? =
            sendRequestAndAwaitResponse(
                SubscriptionRequest(
                    topic,
                    parameter,
                    subscriberId,
                ),
            )
        require(response is SubscriptionResponse)
        log.i {
            "Received SubscriptionResponse for topic $topic and subscriberId $subscriberId."
        }
        require(response.errorMessage == null) {
            "Subscribe for topic $topic and subscriberId $subscriberId failed with error: ${response.errorMessage}"
        }
        webSocketEventObservers[subscriberId] = webSocketEventObserver
        val webSocketEvent =
            WebSocketEvent(
                topic,
                subscriberId,
                response.payload,
                ModificationType.REPLACE,
                0,
            )
        webSocketEventObserver.setEvent(webSocketEvent)
        log.i { "Subscription for $topic and subscriberId $subscriberId completed." }
        return webSocketEventObserver
    }

    override suspend fun unSubscribe(
        topic: Topic,
        requestId: String,
    ) {
        log.w { "unSubscribe not yet implemented for topic: $topic, requestId: $requestId" }
        // TODO: Implement unsubscribe logic
    }

    private suspend fun send(message: WebSocketMessage) {
        // Fail now rather than drop the message and let the request time out
        val currentSession = session ?: throw WebSocketSessionClosedEarly()
        val isQuiet = message is WebSocketRestApiRequest && message.path in QUIET_PATHS
        if (isQuiet) {
            quietRequestIds.add(message.requestId)
        }
        if (!isQuiet) {
            log.d { "Send message ${HeaderRedaction.redactForLogging(message)}" }
        }
        val jsonString: String = json.encodeToString(message)
        if (!isQuiet) {
            log.d { "Send raw text ${HeaderRedaction.redactRawJsonForLogging(jsonString)}" }
        }
        currentSession.send(Frame.Text(jsonString))
    }

    private suspend fun startListening(session: WebSocketSession) {
        var error: Throwable? = null
        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    try {
                        val message = frame.readText()

                        // Extract requestId from raw JSON to avoid SIGSEGV on Kotlin/Native
                        // when accessing properties on polymorphically-deserialized sealed objects.
                        // kotlinx-serialization 1.10.0 + Kotlin/Native 2.3.20 can produce objects
                        // with corrupted string fields that crash on property access.
                        val jsonElement: JsonObject? =
                            try {
                                json.parseToJsonElement(message).jsonObject
                            } catch (_: Exception) {
                                null
                            }
                        val rawRequestId = jsonElement?.get("requestId")?.jsonPrimitive?.contentOrNull

                        val isQuietResponse =
                            rawRequestId != null && quietRequestIds.remove(rawRequestId)
                        if (!isQuietResponse) {
                            log.d { "Received raw text ${HeaderRedaction.redactRawJsonForLogging(message)}" }
                        }

                        val webSocketMessage: WebSocketMessage =
                            json.decodeFromString(
                                WebSocketMessage.serializer(),
                                message,
                            )
                        if (!isQuietResponse) {
                            log.d { "Received webSocketMessage ${HeaderRedaction.redactForLogging(webSocketMessage)}" }
                        }
                        if (webSocketMessage is WebSocketResponse) {
                            onWebSocketResponse(webSocketMessage, rawRequestId)
                        } else if (webSocketMessage is WebSocketEvent) {
                            onWebSocketEvent(webSocketMessage)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.e(e) { "Failed to process incoming WebSocket frame" }
                    }
                }
            }

            // If we get here, the websocket was closed by server,
            // as we cancel the coroutine before reaching here
            log.d { "WebSocket session closed by server" }
            throw WebSocketSessionClosedByServer()
        } catch (e: Throwable) {
            log.e(e) { "Exception occurred whilst listening for WS messages" }
            error = e
        } finally {
            // Dispose request/response handlers
            requestResponseHandlersMutex.withLock {
                requestResponseHandlers.values.forEach { it.dispose() }
                requestResponseHandlers.clear()
            }
            // Clear subscriptions
            webSocketEventObservers.clear()
            _nodeInfo.value = null
            // Set the connection status
            if (isReconnecting.value) {
                _webSocketClientStatus.value =
                    ConnectionState.Disconnected(WebSocketIsReconnecting())
            } else {
                _webSocketClientStatus.value =
                    ConnectionState.Disconnected(error)
            }
        }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun onWebSocketResponse(
        response: WebSocketResponse,
        safeRequestId: String?,
    ) {
        // Use the pre-extracted requestId from raw JSON to avoid SIGSEGV on Kotlin/Native
        // when accessing properties on polymorphically-deserialized sealed objects.
        val requestId = safeRequestId
        if (requestId == null) {
            log.w { "Received WebSocketResponse with null requestId, ignoring" }
            return
        }
        requestResponseHandlers[requestId]?.onWebSocketResponse(response, requestId)
    }

    private suspend fun onWebSocketEvent(event: WebSocketEvent) {
        // We have the payload not serialized yet as we would not know the expected type.
        // We delegate that at the caller who is aware of the expected type
        val webSocketEventObserver = webSocketEventObservers[event.subscriberId]
        if (webSocketEventObserver != null) {
            webSocketEventObserver.setEvent(event)
        } else {
            log.w { "We received a WebSocketEvent but no webSocketEventObserver was found for subscriberId ${event.subscriberId}" }
        }
    }

    /** Reads `/info`; a node that predates it (404) is read through `/settings/version`, without features. */
    private suspend fun fetchNodeInfo(): ApiInfoDto {
        if (!infoEndpointAbsent) {
            val infoResponse = getRest(INFO_PATH)
            if (infoResponse.httpStatusCode != HttpStatusCode.NotFound) {
                val info = json.decodeFromString<ApiInfoDto>(infoResponse.bodyOrThrow(INFO_PATH))
                log.d { "Handshake via $INFO_PATH (features=${info.features?.size})" }
                return info
            }
            infoEndpointAbsent = true
        }
        log.d { "Node predates $INFO_PATH; using $HEALTH_CHECK_PATH" }
        val legacy = json.decodeFromString<ApiVersionSettingsVO>(getRest(HEALTH_CHECK_PATH).bodyOrThrow(HEALTH_CHECK_PATH))
        return ApiInfoDto(legacy.version, legacy.imageVersion)
    }

    private suspend fun getRest(path: String): WebSocketRestApiResponse {
        // Only the version check carries the session and client headers: a node before bisq-api 2.1.14 reads them
        // from the message and answers 401 without them, which would hide its version behind a session renewal.
        // TODO remove these headers in a later bisq-api release, once 2.1.14 nodes are established and the older
        //  ones no longer need the version error.
        val headers =
            buildMap {
                sessionId?.let { put(Headers.SESSION_ID, it) }
                clientId?.let { put(Headers.CLIENT_ID, it) }
            }
        val response = sendRequestAndAwaitResponse(WebSocketRestApiRequest(createUuid(), "GET", path, "", headers), false)
        require(response is WebSocketRestApiResponse) { "Response not of expected type. response=$response" }
        if (response.httpStatusCode == HttpStatusCode.Unauthorized ||
            response.httpStatusCode == HttpStatusCode.Forbidden
        ) {
            // 401: session expired. 403: client revoked (profile deleted, permissions gone).
            // Both mean credentials are no longer valid — stop retrying and trigger session renewal.
            throw UnauthorizedApiAccessException()
        }
        return response
    }

    private fun WebSocketRestApiResponse.bodyOrThrow(path: String): String {
        check(isSuccess()) { "GET $path failed with status $httpStatusCode" }
        check(body.isNotBlank()) { "GET $path returned an empty body" }
        return body
    }

    private fun isApiCompatible(nodeApiVersion: String): Boolean {
        val requiredVersion = BuildConfig.BISQ_API_VERSION
        log.d { "required trusted node api version is $requiredVersion and current is $nodeApiVersion" }
        return try {
            SemanticVersion.from(nodeApiVersion) >=
                SemanticVersion.from(
                    requiredVersion,
                )
        } catch (e: Throwable) {
            log.e(e) { "Failed to parse nodeApiVersion or requiredVersion into a sematic version for comparison" }
            false
        }
    }

    override suspend fun dispose() {
        disconnect()
        stopFlow.tryEmit(Unit)
        clientScope.cancel(CancellationException("WebSocket client disposed"))
    }
}
