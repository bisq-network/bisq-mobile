package network.bisq.mobile.domain.model.account.fiat

data class UpiAccount(
    override val accountName: String,
    override val accountPayload: UpiAccountPayload,
) : FiatAccount
