package network.bisq.mobile.domain.model.account.fiat

data class CashDepositAccount(
    override val accountName: String,
    override val accountPayload: CashDepositAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : FiatPaymentAccount
