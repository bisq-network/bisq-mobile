package network.bisq.mobile.domain.model.account.fiat

data class SwishAccount(
    override val accountName: String,
    override val accountPayload: SwishAccountPayload,
) : FiatAccount
