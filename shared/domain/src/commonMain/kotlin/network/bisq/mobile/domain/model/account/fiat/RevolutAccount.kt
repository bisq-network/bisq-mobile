package network.bisq.mobile.domain.model.account.fiat

data class RevolutAccount(
    override val accountName: String,
    override val accountPayload: RevolutAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : FiatPaymentAccount
