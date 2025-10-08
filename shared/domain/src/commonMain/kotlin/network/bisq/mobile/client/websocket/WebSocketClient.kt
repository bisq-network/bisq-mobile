package network.bisq.mobile.client.websocket

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver

interface WebSocketClient {
    val host: String
    val port: Int
    val webSocketClientStatus: StateFlow<ConnectionState>

    fun isConnected(): Boolean = webSocketClientStatus.value is ConnectionState.Connected

    fun isDemo(): Boolean

    suspend fun connect(timeout: Long = 10000L): Throwable?

    /**
     * @param isReconnect true if this was called from a reconnect method
     */
    suspend fun disconnect(isReconnect: Boolean = false)

    fun reconnect()

    suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest, awaitConnection: Boolean = true): WebSocketResponse?

    /** Suspends until `webSocketClientStatus` is `ConnectionState.Connected`. Use with caution, may suspend indefinitely. */
    suspend fun awaitConnection()

    suspend fun subscribe(topic: Topic, parameter: String? = null): WebSocketEventObserver

    suspend fun unSubscribe(topic: Topic, requestId: String)
}