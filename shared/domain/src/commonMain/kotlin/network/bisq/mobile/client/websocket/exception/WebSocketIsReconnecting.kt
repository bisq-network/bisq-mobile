package network.bisq.mobile.client.websocket.exception

data class WebSocketIsReconnecting(
    override val cause: Throwable? = null
) : RuntimeException("WebSocket is trying to reconnect", cause) {
    override val message: String
        get() = super.message ?: "WebSocket is trying to reconnect"  // Fallback to avoid null, just in case
}