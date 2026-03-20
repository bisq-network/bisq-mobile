package network.bisq.mobile.domain.model.account.fiat

data class DomesticWireTransferAccount(
    override val accountName: String,
    override val accountPayload: DomesticWireTransferAccountPayload,
) : FiatAccount
