package network.bisq.mobile.domain.model.account.fiat

data class ImpsAccount(
    override val accountName: String,
    override val accountPayload: ImpsAccountPayload,
) : FiatAccount
