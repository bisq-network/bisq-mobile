package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.util.collections.ConcurrentMap
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.network.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.client.network.exception.MaximumRetryReachedException
import network.bisq.mobile.client.service.settings.SettingsUtils
import network.bisq.mobile.client.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.utils.Logging
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

    // Add these properties to track reconnection state
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

    private val _webSocketClientStatus = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    override val webSocketClientStatus: StateFlow<ConnectionState> get() = _webSocketClientStatus.asStateFlow()

    private var listenerJob: Job? = null

    override fun isDemo(): Boolean = false

    override suspend fun connect(timeoutMs: Long): Throwable? {
        connectionMutex.withLock {
            try {
                if (isConnected() || isConnecting()) {
                    return null
                }
                log.d { "WS connecting.." }
                _webSocketClientStatus.value = ConnectionState.Connecting
                var serverVersion: String?
                try {
                    val resp = withTimeout(timeoutMs) {
                        httpClient.get("/api/v1/settings/version").bodyAsText()
                    }
                    serverVersion = json.decodeFromString<Map<String, String>>(resp).getOrElse("version") { null }
                    if (serverVersion.isNullOrBlank()) {
                        throw IllegalStateException("Expected server version to be provided at /api/v1/settings/version, but was not. response: $resp")
                    }
                } catch (e: Throwable) {
                    throw IllegalStateException("Unexpected error while retrieving http api server version", e)
                }
                if (!SettingsUtils.isApiCompatible(serverVersion)) {
                    throw IncompatibleHttpApiVersionException(serverVersion)
                }
                val newSession = withTimeout(timeoutMs) {
                    httpClient.webSocketSession { url(webSocketUrl) }
                }
                session = newSession
                if (newSession.isActive) {
                    log.d { "WS connected successfully" }
                    listenerJob = ioScope.launch { startListening() }
                    _webSocketClientStatus.value = ConnectionState.Connected

                    // Reset reconnect attempts on successful connection
                    reconnectAttempts = 0
                }
            } catch (e: IllegalStateException) {
                log.w(e) { "WS connection attempt ignored" }
                _webSocketClientStatus.value = ConnectionState.Disconnected(e)
                return e
            } catch (e: Exception) {
                log.e(e) { "WS connection failed to connect to $webSocketUrl" }
                _webSocketClientStatus.value = ConnectionState.Disconnected(e)
                return e
            }
            return null
        }
    }

    override suspend fun disconnect(isReconnect: Boolean) {
        connectionMutex.withLock {
            log.d { "disconnecting socket: isReconnected=$isReconnect" }
            if (!isReconnect) {
                reconnectJob?.cancel()
                reconnectJob = null
            }
            listenerJob?.cancel()
            listenerJob = null
            requestResponseHandlersMutex.withLock {
                requestResponseHandlers.values.forEach { it.dispose() }
                requestResponseHandlers.clear()
            }

            session?.close()
            session = null
            _webSocketClientStatus.value = ConnectionState.Disconnected()
            log.d { "WS disconnected" }
        }
    }

    override fun reconnect() {
        if (isReconnecting.getAndSet(true)) {
            log.d { "Reconnect already in progress, skipping" }
            return
        }
        reconnectJob?.cancel()

        val newReconnectJob = ioScope.launch {
            log.d { "Launching reconnect attempt #${reconnectAttempts + 1}" }

            // Implement exponential backoff
            val delayMillis = minOf(
                DELAY_TO_RECONNECT * (1 shl minOf(reconnectAttempts, 4)), // Exponential backoff
                MAX_RECONNECT_DELAY
            )

            log.d { "Waiting ${delayMillis}ms before reconnect attempt" }
            disconnect(true)
            delay(delayMillis)

            // Check if we've exceeded max attempts
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                val e = MaximumRetryReachedException(MAX_RECONNECT_ATTEMPTS)
                log.w { e.message }
                _webSocketClientStatus.value = ConnectionState.Disconnected(e)
                // Reset counter for future reconnects
                reconnectAttempts = 0
                return@launch
            }

            reconnectAttempts++
            connect()
        }
        reconnectJob = newReconnectJob
        newReconnectJob.invokeOnCompletion {
            isReconnecting.value = false
        }
    }

    // Blocking request until we get the associated response
    override suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse? {
        awaitConnection()

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

    override suspend fun awaitConnection() {
        webSocketClientStatus.first { it is ConnectionState.Connected }
    }

    override suspend fun subscribe(topic: Topic, parameter: String?): WebSocketEventObserver {
        val subscriberId = createUuid()
        log.i { "Subscribe for topic $topic and subscriberId $subscriberId" }

        val subscriptionRequest = SubscriptionRequest(
            subscriberId,
            topic,
            parameter
        )
        val response: WebSocketResponse? = sendRequestAndAwaitResponse(subscriptionRequest)
        require(response is SubscriptionResponse)
        log.i {
            "Received SubscriptionResponse for topic $topic and subscriberId $subscriberId."
        }
        val webSocketEventObserver = WebSocketEventObserver()
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
        awaitConnection()
        log.i { "Send message $message" }
        val jsonString: String = json.encodeToString(message)
        log.i { "Send raw text $jsonString" }
        session?.send(Frame.Text(jsonString))
    }

    private suspend fun startListening() {
        session?.let { session ->
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
                
                // If we get here, the loop exited normally (session closed gracefully)
                log.d { "WebSocket session closed normally" }
                _webSocketClientStatus.value = ConnectionState.Disconnected()
                
            } catch (e: Exception) {
                log.e(e) { "Exception occurred whilst listening for WS messages - triggering reconnect" }
                // Only reconnect on exception
//                TODO this needs more work
//                reconnect()
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
}