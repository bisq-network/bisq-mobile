package network.bisq.mobile.domain.model.account.fiat

data class SepaInstantAccount(
    override val accountName: String,
    override val accountPayload: SepaInstantAccountPayload,
) : FiatAccount
