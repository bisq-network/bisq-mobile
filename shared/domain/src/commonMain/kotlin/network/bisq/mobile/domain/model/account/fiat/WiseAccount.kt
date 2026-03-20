package network.bisq.mobile.domain.model.account.fiat

data class WiseAccount(
    override val accountName: String,
    override val accountPayload: WiseAccountPayload,
) : FiatAccount
