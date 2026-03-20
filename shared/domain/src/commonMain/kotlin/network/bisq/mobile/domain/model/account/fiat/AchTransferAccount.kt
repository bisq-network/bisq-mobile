package network.bisq.mobile.domain.model.account.fiat

data class AchTransferAccount(
    override val accountName: String,
    override val accountPayload: AchTransferAccountPayload,
) : FiatAccount
