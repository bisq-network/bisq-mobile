package network.bisq.mobile.domain.model.account.fiat

data class MoneyBeamAccount(
    override val accountName: String,
    override val accountPayload: MoneyBeamAccountPayload,
) : FiatAccount
