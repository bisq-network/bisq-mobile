package network.bisq.mobile.domain.model.account.fiat

data class SwiftAccount(
    override val accountName: String,
    override val accountPayload: SwiftAccountPayload,
) : FiatAccount
