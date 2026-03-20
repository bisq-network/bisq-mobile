package network.bisq.mobile.domain.model.account.fiat

data class NeftAccount(
    override val accountName: String,
    override val accountPayload: NeftAccountPayload,
) : FiatAccount
