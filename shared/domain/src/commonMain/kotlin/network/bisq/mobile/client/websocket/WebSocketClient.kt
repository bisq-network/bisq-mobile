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

    suspend fun connect(isTest: Boolean = false)

    /**
     * @param isReconnect true if this was called from a reconnect method
     */
    suspend fun disconnect(isTest: Boolean = false, isReconnect: Boolean = false)

    fun reconnect()

    suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse?

    /** Suspends until `webSocketClientStatus` is `ConnectionState.Connected`. Use with caution, may suspend indefinitely. */
    suspend fun awaitConnection()

    suspend fun subscribe(topic: Topic, parameter: String? = null): WebSocketEventObserver

    suspend fun unSubscribe(topic: Topic, requestId: String)
}