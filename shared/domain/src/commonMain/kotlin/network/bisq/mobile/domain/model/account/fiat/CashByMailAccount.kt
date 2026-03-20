package network.bisq.mobile.domain.model.account.fiat

data class CashByMailAccount(
    override val accountName: String,
    override val accountPayload: CashByMailAccountPayload,
) : FiatAccount
