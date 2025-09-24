package network.bisq.mobile.client.websocket.exception

/**
 * Exception thrown when the maximum number of retry attempts is reached.
 *
 * @param attempts The number of attempts made.
 * @param cause The cause of this exception.
 */
data class MaximumRetryReachedException(
    val attempts: Int,
    override val cause: Throwable? = null
) : Exception("Maximum reconnect attempts ($attempts) reached", cause) {
    override val message: String
        get() = super.message ?: "Maximum reconnect attempts ($attempts) reached"  // Fallback to avoid null, just in case
}