package network.bisq.mobile.domain.model.account.fiat

data class StrikeAccount(
    override val accountName: String,
    override val accountPayload: StrikeAccountPayload,
) : FiatAccount
