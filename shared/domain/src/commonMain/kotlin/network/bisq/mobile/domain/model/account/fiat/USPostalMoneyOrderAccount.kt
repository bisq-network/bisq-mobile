package network.bisq.mobile.domain.model.account.fiat

data class USPostalMoneyOrderAccount(
    override val accountName: String,
    override val accountPayload: USPostalMoneyOrderAccountPayload,
) : FiatAccount
