package network.bisq.mobile.domain.model.account.fiat

data class PerfectMoneyAccount(
    override val accountName: String,
    override val accountPayload: PerfectMoneyAccountPayload,
) : FiatAccount
