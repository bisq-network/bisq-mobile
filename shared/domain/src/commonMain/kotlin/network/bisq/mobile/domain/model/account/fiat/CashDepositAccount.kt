package network.bisq.mobile.domain.model.account.fiat

data class CashDepositAccount(
    override val accountName: String,
    override val accountPayload: CashDepositAccountPayload,
) : FiatAccount
