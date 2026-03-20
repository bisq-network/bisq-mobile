package network.bisq.mobile.domain.model.account.fiat

data class FasterPaymentsAccount(
    override val accountName: String,
    override val accountPayload: FasterPaymentsAccountPayload,
) : FiatAccount
