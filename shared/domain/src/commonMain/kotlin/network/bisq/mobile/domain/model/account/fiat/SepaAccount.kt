package network.bisq.mobile.domain.model.account.fiat

data class SepaAccount(
    override val accountName: String,
    override val accountPayload: SepaAccountPayload,
) : FiatAccount
