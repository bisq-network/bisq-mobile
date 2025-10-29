package network.bisq.mobile.client.httpclient.exception

data class PasswordIncorrectOrMissingException(
    override val cause: Throwable? = null
) : RuntimeException("Password incorrect or missing", cause) {
    override val message: String
        get() = super.message
            ?: "Password incorrect or missing"  // Fallback to avoid null, just in case
}