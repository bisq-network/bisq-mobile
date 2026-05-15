package network.bisq.mobile.client.common.domain.websocket

import io.ktor.http.Url
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver

interface WebSocketClient {
    companion object {
        const val CLEARNET_CONNECT_TIMEOUT = 15_000L
        const val TOR_CONNECT_TIMEOUT = 60_000L

        fun determineTimeout(host: String): Long =
            if (host.contains(".onion")) {
                TOR_CONNECT_TIMEOUT
            } else {
                CLEARNET_CONNECT_TIMEOUT
            }
    }

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

    suspend fun unSubscribe(
        topic: Topic,
        requestId: String,
    )

    /**
     * Synchronously cancels the reconnect machinery (reconnect job, internal
     * coroutine scope) WITHOUT acquiring [connectionMutex] or running the full
     * [dispose] sequence.
     *
     * Must be invoked by the owning service BEFORE it forcibly invalidates the
     * underlying iOS NSURLSession (e.g. inside `forceClientRecreation`).
     * Otherwise the in-flight reconnect job's `invokeOnCompletion` may, upon
     * receiving the invalidate-triggered cancellation, recursively call
     * [reconnect] → [connect] → `httpClient.webSocketSession { ... }` →
     * `NSURLSession.webSocketTaskForRequest(...)` on the now-invalidated
     * session, which raises an uncatchable `NSGenericException: 'Task created
     * in a session that has been invalidated'` and crashes the app.
     */
    fun prepareForRecreation()

    suspend fun dispose()
}
