package network.bisq.mobile.client.websocket.exception

data class WebSocketSessionClosedEarly(
    override val cause: Throwable? = null
) : RuntimeException("WebSocket session was closed unexpectedly", cause) {
    override val message: String
        get() = super.message ?: "WebSocket session was closed unexpectedly"  // Fallback to avoid null, just in case
}