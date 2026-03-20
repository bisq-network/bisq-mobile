package network.bisq.mobile.domain.model.account.fiat

data class AliPayAccount(
    override val accountName: String,
    override val accountPayload: AliPayAccountPayload,
) : FiatAccount
