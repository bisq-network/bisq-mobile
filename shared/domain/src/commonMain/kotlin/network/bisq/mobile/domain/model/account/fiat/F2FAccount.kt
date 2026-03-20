package network.bisq.mobile.domain.model.account.fiat

data class F2FAccount(
    override val accountName: String,
    override val accountPayload: F2FAccountPayload,
) : FiatAccount
