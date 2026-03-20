package network.bisq.mobile.domain.model.account.fiat

data class MercadoPagoAccount(
    override val accountName: String,
    override val accountPayload: MercadoPagoAccountPayload,
) : FiatAccount
