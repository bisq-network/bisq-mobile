package network.bisq.mobile.domain.model.account.fiat

data class ZelleAccount(
    override val accountName: String,
    override val accountPayload: ZelleAccountPayload,
    override val creationDate: String?,
    override val tradeLimitInfo: String?,
    override val tradeDuration: String?,
) : FiatPaymentAccount
