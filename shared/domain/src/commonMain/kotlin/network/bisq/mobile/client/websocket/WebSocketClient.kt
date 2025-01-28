package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.util.collections.ConcurrentMap
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.createUuid

class WebSocketClient(
    private val httpClient: HttpClient,
    val json: Json,
    val host: String,
    val port: Int
) : Logging {

    companion object {
        const val DELAY_TO_RECONNECT = 3000L
    }

    private val webSocketUrl: String = "ws://$host:$port/websocket"
    private var session: DefaultClientWebSocketSession? = null
    private val webSocketEventObservers = ConcurrentMap<String, WebSocketEventObserver>()
    private val requestResponseHandlers = mutableMapOf<String, RequestResponseHandler>()
    private var connectionReady = CompletableDeferred<Boolean>()
    private val requestResponseHandlersMutex = Mutex()

    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    enum class WebSockectClientStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    val _connected = MutableStateFlow(WebSockectClientStatus.DISCONNECTED)
    val connected: StateFlow<WebSockectClientStatus> = _connected

    fun isConnected(): Boolean = connected.value == WebSockectClientStatus.CONNECTED

    suspend fun connect(isTest: Boolean = false) {
        log.i("Connecting to websocket at: $webSocketUrl")
        if (connected.value != WebSockectClientStatus.CONNECTED) {
            try {
                _connected.value = WebSockectClientStatus.CONNECTING
                session = httpClient.webSocketSession { url(webSocketUrl) }
                if (session?.isActive == true) {
                    _connected.value = WebSockectClientStatus.CONNECTED
                    CoroutineScope(BackgroundDispatcher).launch { startListening() }
                    connectionReady.complete(true)
                    if (!isTest) {
                        log.d { "Websocket connected" }
                    }
                }
            } catch (e: Exception) {
                log.e("Connecting websocket failed", e)
                _connected.value = WebSockectClientStatus.DISCONNECTED
                if (isTest) {
                    throw e
                } else {
                    reconnect()
                }
            }
        }
    }

    suspend fun disconnect(isTest: Boolean = false) {
        requestResponseHandlersMutex.withLock {
            requestResponseHandlers.values.forEach { it.dispose() }
            requestResponseHandlers.clear()
        }

        session?.close()
        session = null
        _connected.value = WebSockectClientStatus.DISCONNECTED
        if (!isTest) {
            log.d { "WS disconnected" }
        }
    }

    private fun reconnect() {
        backgroundScope.launch {
            log.d { "Launching reconnect" }
            disconnect()
            delay(DELAY_TO_RECONNECT) // Delay before reconnecting
            connect()  // Try reconnecting recursively
        }
    }

    // Blocking request until we get the associated response
    suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse? {
        connectionReady.await()

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


    suspend fun subscribe(topic: Topic, parameter: String? = null): WebSocketEventObserver {
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

    suspend fun unSubscribe(topic: Topic, requestId: String) {
        //todo
    }

    private suspend fun send(message: WebSocketMessage) {
        connectionReady.await()
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
            } catch (e: Exception) {
                log.e(e) { "Exception ocurred whilst listening for WS messages - triggering reconnect" }
            } finally {
                log.d { "Not listening for WS messages anymore - launching reconnect" }
                reconnect()
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

    /* object ServerEventSerializer :
         JsonTransformingSerializer<WebSocketMessage>(WebSocketMessage.serializer()) {
         override fun transformDeserialize(element: JsonElement): JsonElement {
             // Ignore deferredPayload at deserialization as we do not know that type at that
             // moment
             return JsonObject(element.jsonObject.filterKeys { it != "deferredPayload" })
         }
     }*/
}