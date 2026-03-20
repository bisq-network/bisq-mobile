package network.bisq.mobile.domain.model.account.fiat

data class Pin4Account(
    override val accountName: String,
    override val accountPayload: Pin4AccountPayload,
) : FiatAccount
