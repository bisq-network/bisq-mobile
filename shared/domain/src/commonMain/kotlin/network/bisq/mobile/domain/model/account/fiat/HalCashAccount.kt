package network.bisq.mobile.domain.model.account.fiat

data class HalCashAccount(
    override val accountName: String,
    override val accountPayload: HalCashAccountPayload,
) : FiatAccount
