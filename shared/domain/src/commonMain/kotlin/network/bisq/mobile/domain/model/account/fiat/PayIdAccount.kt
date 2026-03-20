package network.bisq.mobile.domain.model.account.fiat

data class PayIdAccount(
    override val accountName: String,
    override val accountPayload: PayIdAccountPayload,
) : FiatAccount
