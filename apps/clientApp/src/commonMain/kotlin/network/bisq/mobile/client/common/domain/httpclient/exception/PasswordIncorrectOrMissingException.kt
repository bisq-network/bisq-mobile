package network.bisq.mobile.client.common.domain.httpclient.exception

data class PasswordIncorrectOrMissingException(
    override val cause: Throwable? = null,
) : RuntimeException("Password incorrect or missing", cause) {
    override val message: String
        get() = super.message!!
}
