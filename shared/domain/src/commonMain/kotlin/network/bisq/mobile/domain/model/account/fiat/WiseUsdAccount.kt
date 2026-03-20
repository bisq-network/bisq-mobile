package network.bisq.mobile.domain.model.account.fiat

data class WiseUsdAccount(
    override val accountName: String,
    override val accountPayload: WiseUsdAccountPayload,
) : FiatAccount
