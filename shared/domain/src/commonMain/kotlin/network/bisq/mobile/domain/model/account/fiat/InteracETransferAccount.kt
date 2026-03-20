package network.bisq.mobile.domain.model.account.fiat

data class InteracETransferAccount(
    override val accountName: String,
    override val accountPayload: InteracETransferAccountPayload,
) : FiatAccount
