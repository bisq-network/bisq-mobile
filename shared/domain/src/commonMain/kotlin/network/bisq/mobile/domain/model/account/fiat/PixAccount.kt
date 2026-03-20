package network.bisq.mobile.domain.model.account.fiat

data class PixAccount(
    override val accountName: String,
    override val accountPayload: PixAccountPayload,
) : FiatAccount
