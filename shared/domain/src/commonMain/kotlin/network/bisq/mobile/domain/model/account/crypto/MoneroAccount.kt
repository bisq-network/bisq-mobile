package network.bisq.mobile.domain.model.account.crypto

data class MoneroAccount(
    override val accountName: String,
    override val accountPayload: MoneroAccountPayload,
    override val creationDate: String?,
    override val tradeLimitInfo: String?,
    override val tradeDuration: String?,
) : CryptoPaymentAccount
