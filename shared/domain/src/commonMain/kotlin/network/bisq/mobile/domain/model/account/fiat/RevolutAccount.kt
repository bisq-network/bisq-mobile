package network.bisq.mobile.domain.model.account.fiat

data class RevolutAccount(
    override val accountName: String,
    override val accountPayload: RevolutAccountPayload,
) : FiatAccount
