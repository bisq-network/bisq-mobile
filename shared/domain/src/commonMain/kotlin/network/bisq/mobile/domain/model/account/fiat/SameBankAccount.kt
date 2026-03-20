package network.bisq.mobile.domain.model.account.fiat

data class SameBankAccount(
    override val accountName: String,
    override val accountPayload: SameBankAccountPayload,
) : FiatAccount
