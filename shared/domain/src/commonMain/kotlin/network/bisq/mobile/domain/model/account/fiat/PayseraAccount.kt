package network.bisq.mobile.domain.model.account.fiat

data class PayseraAccount(
    override val accountName: String,
    override val accountPayload: PayseraAccountPayload,
) : FiatAccount
