package network.bisq.mobile.domain.model.account.fiat

data class WiseAccount(
    override val accountName: String,
    override val accountPayload: WiseAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : FiatPaymentAccount
