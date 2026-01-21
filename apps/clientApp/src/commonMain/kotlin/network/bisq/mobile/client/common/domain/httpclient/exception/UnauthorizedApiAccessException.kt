package network.bisq.mobile.client.common.domain.httpclient.exception

data class UnauthorizedApiAccessException(
    override val cause: Throwable? = null,
) : RuntimeException("Api access was not authorized", cause) {
    override val message: String
        get() = super.message!!
}
