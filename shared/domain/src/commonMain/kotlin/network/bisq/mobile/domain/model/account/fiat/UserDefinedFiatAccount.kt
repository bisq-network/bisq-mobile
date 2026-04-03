package network.bisq.mobile.domain.model.account.fiat

data class UserDefinedFiatAccount(
    override val accountName: String,
    override val accountPayload: UserDefinedFiatAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : FiatPaymentAccount
