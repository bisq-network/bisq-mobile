package network.bisq.mobile.domain.model.account.fiat

data class SatispayAccount(
    override val accountName: String,
    override val accountPayload: SatispayAccountPayload,
) : FiatAccount
