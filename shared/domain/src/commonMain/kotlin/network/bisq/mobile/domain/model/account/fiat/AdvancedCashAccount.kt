package network.bisq.mobile.domain.model.account.fiat

data class AdvancedCashAccount(
    override val accountName: String,
    override val accountPayload: AdvancedCashAccountPayload,
) : FiatAccount
