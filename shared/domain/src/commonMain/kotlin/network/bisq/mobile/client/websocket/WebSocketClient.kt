package network.bisq.mobile.client.websocket

import io.ktor.http.Url
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver

interface WebSocketClient {
    val apiUrl: Url
    val webSocketClientStatus: StateFlow<ConnectionState>

    fun isConnected(): Boolean = webSocketClientStatus.value is ConnectionState.Connected

    fun isDemo(): Boolean

    suspend fun connect(timeout: Long = 10000L): Throwable?

    suspend fun disconnect()

    fun reconnect()

    suspend fun sendRequestAndAwaitResponse(
        webSocketRequest: WebSocketRequest,
        awaitConnection: Boolean = true,
    ): WebSocketResponse?

    suspend fun subscribe(
        topic: Topic,
        parameter: String? = null,
        webSocketEventObserver: WebSocketEventObserver = WebSocketEventObserver(),
    ): WebSocketEventObserver

    suspend fun unSubscribe(topic: Topic, requestId: String)

    suspend fun dispose()
}
