package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.util.collections.ConcurrentMap
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.client.websocket.exception.MaximumRetryReachedException
import network.bisq.mobile.client.websocket.exception.WebSocketIsReconnecting
import network.bisq.mobile.client.websocket.exception.WebSocketSessionClosedByServer
import network.bisq.mobile.client.websocket.exception.WebSocketSessionClosedEarly
import network.bisq.mobile.client.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.settings.ApiVersionSettingsVO
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.SemanticVersion
import network.bisq.mobile.domain.utils.createUuid

class WebSocketClientImpl(
    private val httpClient: HttpClient,
    private val json: Json,
    override val host: String,
    override val port: Int
) : WebSocketClient, Logging {

    companion object {
        const val DELAY_TO_RECONNECT = 3000L
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val MAX_RECONNECT_DELAY = 30000L // 30 seconds max delay
    }

    private var reconnectAttempts = 0
    private var isReconnecting = atomic(false)
    private var reconnectJob: Job? = null

    private val webSocketUrl: String = "ws://$host:$port/websocket"
    private var session: DefaultClientWebSocketSession? = null
    private val webSocketEventObservers = ConcurrentMap<String, WebSocketEventObserver>()
    private val requestResponseHandlers = mutableMapOf<String, RequestResponseHandler>()
    private val connectionMutex = Mutex()
    private val requestResponseHandlersMutex = Mutex()

    private val ioScope = CoroutineScope(IODispatcher)

    private val _webSocketClientStatus =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    override val webSocketClientStatus: StateFlow<ConnectionState> get() = _webSocketClientStatus.asStateFlow()

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
                doDisconnect() // clean up state
                log.d { "WS connecting.." }
                _webSocketClientStatus.value = ConnectionState.Connecting
                val startTime = DateUtils.now()
                val newSession = withContext(IODispatcher) {
                    withTimeout(timeout) {
                        httpClient.webSocketSession { url(webSocketUrl) }
                    }
                }
                val elapsed = DateUtils.now() - startTime
                val remainingTime = timeout - elapsed
                session = newSession
                if (newSession.isActive) {
                    log.d { "WS connected successfully" }
                    listenerJob = ioScope.launch { startListening(newSession) }

                    withContext(IODispatcher) {
                        withTimeout(remainingTime) {
                            val nodeApiVersion = getApiVersion()
                            if (!isApiCompatible(nodeApiVersion)) {
                                doDisconnect()
                                awaitDisconnection() // so that we update the disconnect reason correctly
                                throw IncompatibleHttpApiVersionException(nodeApiVersion.version)
                            }
                        }
                    }

                    _webSocketClientStatus.value = ConnectionState.Connected

                    // Reset reconnect attempts on successful connection
                    reconnectAttempts = 0
                } else {
                    throw WebSocketSessionClosedEarly()
                }
            } catch (e: Exception) {
                log.e(e) { "WS connection failed to connect to $webSocketUrl" }
                _webSocketClientStatus.value = ConnectionState.Disconnected(e)
                return e
            }
            return null
        }
    }

    override suspend fun disconnect() {
        connectionMutex.withLock {
            log.d { "WS disconnect called" }
            doDisconnect()
        }
    }

    /**
     * Disconnect and cleanup logic, without the lock for internal calls
     */
    private suspend fun doDisconnect() {
        // order is important. we want to know the difference between server close and our close
        // so we cancel the listen job so it throws cancellation exception first then cose the session
        listenerJob?.cancel()
        listenerJob = null
        session?.close()
        session = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun reconnect() {
        if (isReconnecting.getAndSet(true)) {
            log.d { "Reconnect already in progress, skipping" }
            return
        }
        reconnectJob?.cancel()

        val newReconnectJob: Deferred<Throwable?> = ioScope.async {
            log.d { "Launching reconnect attempt #${reconnectAttempts + 1}" }
            doDisconnect() // clean up state

            // Check if we've exceeded max attempts
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                val e = MaximumRetryReachedException(MAX_RECONNECT_ATTEMPTS)
                log.w { e.message }
                _webSocketClientStatus.value = ConnectionState.Disconnected(e)
                // Reset counter for future reconnects
                reconnectAttempts = 0
                return@async null
            }

            // Implement exponential backoff
            val delayMillis = minOf(
                DELAY_TO_RECONNECT * (1 shl minOf(reconnectAttempts, 4)), // Exponential backoff
                MAX_RECONNECT_DELAY
            )
            log.d { "Waiting ${delayMillis}ms before reconnect attempt" }
            delay(delayMillis)
            reconnectAttempts++
            val error = connect()
            return@async error
        }
        reconnectJob = newReconnectJob
        newReconnectJob.invokeOnCompletion {
            isReconnecting.value = false
            if (it == null && newReconnectJob.getCompleted() != null) {
                reconnect()
            }
        }
    }

    // Blocking request until we get the associated response
    override suspend fun sendRequestAndAwaitResponse(
        webSocketRequest: WebSocketRequest,
        awaitConnection: Boolean,
    ): WebSocketResponse? {
        if (awaitConnection) {
            awaitConnection()
        }

        val requestId = webSocketRequest.requestId
        val requestResponseHandler = RequestResponseHandler(this::send)
        requestResponseHandlersMutex.withLock {
            requestResponseHandlers[requestId] = requestResponseHandler
        }

        try {
            return requestResponseHandler.request(webSocketRequest)
        } finally {
            requestResponseHandlersMutex.withLock {
                requestResponseHandlers.remove(requestId)
            }
        }
    }

    private suspend fun awaitConnection() {
        withContext(ioScope.coroutineContext) {
            webSocketClientStatus.first { it is ConnectionState.Connected }
        }
    }

    private suspend fun awaitDisconnection() {
        withContext(ioScope.coroutineContext) {
            webSocketClientStatus.first { it is ConnectionState.Disconnected }
        }
    }

    override suspend fun subscribe(
        topic: Topic,
        parameter: String?,
        webSocketEventObserver: WebSocketEventObserver,
    ): WebSocketEventObserver {
        val subscriberId = createUuid()
        log.i { "Subscribe for topic $topic and subscriberId $subscriberId" }

        val response: WebSocketResponse? =
            sendRequestAndAwaitResponse(SubscriptionRequest(topic, parameter, subscriberId))
        require(response is SubscriptionResponse)
        log.i {
            "Received SubscriptionResponse for topic $topic and subscriberId $subscriberId."
        }
        webSocketEventObservers[subscriberId] = webSocketEventObserver
        val webSocketEvent = WebSocketEvent(
            topic,
            subscriberId,
            response.payload,
            ModificationType.REPLACE,
            0
        )
        webSocketEventObserver.setEvent(webSocketEvent)
        log.i { "Subscription for $topic and subscriberId $subscriberId completed." }
        return webSocketEventObserver
    }

    override suspend fun unSubscribe(topic: Topic, requestId: String) {
        log.w { "unSubscribe not yet implemented for topic: $topic, requestId: $requestId" }
        // TODO: Implement unsubscribe logic
    }

    private suspend fun send(message: WebSocketMessage) {
        log.i { "Send message $message" }
        val jsonString: String = json.encodeToString(message)
        log.i { "Send raw text $jsonString" }
        session?.send(Frame.Text(jsonString))
    }

    private suspend fun startListening(session: WebSocketSession) {
        var error: Throwable? = null
        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val message = frame.readText()
                    //todo add input validation
                    log.d { "Received raw text $message" }
                    val webSocketMessage: WebSocketMessage =
                        json.decodeFromString(WebSocketMessage.serializer(), message)
                    log.i { "Received webSocketMessage $webSocketMessage" }
                    if (webSocketMessage is WebSocketResponse) {
                        onWebSocketResponse(webSocketMessage)
                    } else if (webSocketMessage is WebSocketEvent) {
                        onWebSocketEvent(webSocketMessage)
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
            // Set the connection status
            if (isReconnecting.value) {
                _webSocketClientStatus.value =
                    ConnectionState.Disconnected(WebSocketIsReconnecting())
            } else {
                _webSocketClientStatus.value = ConnectionState.Disconnected(error)
            }
        }
    }

    private suspend fun onWebSocketResponse(response: WebSocketResponse) {
        requestResponseHandlers[response.requestId]?.onWebSocketResponse(response)
    }

    private fun onWebSocketEvent(event: WebSocketEvent) {
        // We have the payload not serialized yet as we would not know the expected type.
        // We delegate that at the caller who is aware of the expected type
        val webSocketEventObserver = webSocketEventObservers[event.subscriberId]
        if (webSocketEventObserver != null) {
            webSocketEventObserver.setEvent(event)
        } else {
            log.w { "We received a WebSocketEvent but no webSocketEventObserver was found for subscriberId ${event.subscriberId}" }
        }
    }

    private suspend fun getApiVersion(): ApiVersionSettingsVO {
        val requestId = createUuid()
        val webSocketRestApiRequest = WebSocketRestApiRequest(
            requestId,
            "GET",
            "/api/v1/settings/version",
            "",
        )
        val response = sendRequestAndAwaitResponse(webSocketRestApiRequest, false)
        require(response is WebSocketRestApiResponse) { "Response not of expected type. response=$response" }
        val body = response.body
        val decodeFromString = json.decodeFromString<ApiVersionSettingsVO>(body)
        return decodeFromString
    }

    private fun isApiCompatible(apiVersion: ApiVersionSettingsVO): Boolean {
        val requiredVersion = BuildConfig.BISQ_API_VERSION
        val nodeApiVersion = apiVersion.version
        log.d { "required trusted node api version is $requiredVersion and current is $nodeApiVersion" }
        return try {
            SemanticVersion.from(nodeApiVersion) >= SemanticVersion.from(requiredVersion)
        } catch (e: Throwable) {
            log.e(e) { "Failed to parse nodeApiVersion or requiredVersion into a sematic version for comparison" }
            false
        }
    }

    override suspend fun dispose() {
        disconnect()
        ioScope.cancel(CancellationException("WebSocket client disposed"))
    }
}