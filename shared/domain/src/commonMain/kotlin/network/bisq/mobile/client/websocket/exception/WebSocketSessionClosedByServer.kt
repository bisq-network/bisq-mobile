package network.bisq.mobile.client.websocket.exception

data class WebSocketSessionClosedByServer(
    override val cause: Throwable? = null
) : RuntimeException("WebSocket session was closed by server", cause) {
    override val message: String
        get() = super.message ?: "WebSocket session was closed by server"  // Fallback to avoid null, just in case
}