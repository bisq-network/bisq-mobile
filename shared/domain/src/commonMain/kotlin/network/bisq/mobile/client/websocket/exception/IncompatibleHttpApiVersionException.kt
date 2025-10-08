package network.bisq.mobile.client.websocket.exception


data class IncompatibleHttpApiVersionException(
    val serverVersion: String,
    override val cause: Throwable? = null
) : Exception("Incompatible Http Api Server Version: $serverVersion", cause) {
    override val message: String
        get() = super.message ?: "Incompatible Http Api Server Version: $serverVersion"  // Fallback to avoid null, just in case
}