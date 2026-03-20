package network.bisq.mobile.domain.model.account.fiat

data class UpholdAccount(
    override val accountName: String,
    override val accountPayload: UpholdAccountPayload,
) : FiatAccount
