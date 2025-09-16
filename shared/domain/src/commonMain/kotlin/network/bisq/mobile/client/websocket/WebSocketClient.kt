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
    fun isConnecting(): Boolean = webSocketClientStatus.value is ConnectionState.Connecting

    fun isDemo(): Boolean

    /**
     * Attempts to connect to the websocket server defined by host:port and returns null if successful or already connection.
     * Returns the error otherwise.
     */
    suspend fun connect(timeoutMs: Long = 10000L): Throwable?

    /**
     * @param isReconnect true if this was called from a reconnect method
     */
    suspend fun disconnect(isReconnect: Boolean = false)

    fun reconnect()

    suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse?

    /** Suspends until `webSocketClientStatus` == `CONNECTED`. Use with caution, may suspend indefinitely. */
    suspend fun awaitConnection()

    suspend fun subscribe(topic: Topic, parameter: String? = null): WebSocketEventObserver

    suspend fun unSubscribe(topic: Topic, requestId: String)
}
