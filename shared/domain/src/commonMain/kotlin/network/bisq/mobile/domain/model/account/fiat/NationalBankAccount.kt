package network.bisq.mobile.domain.model.account.fiat

data class NationalBankAccount(
    override val accountName: String,
    override val accountPayload: NationalBankAccountPayload,
) : FiatAccount
