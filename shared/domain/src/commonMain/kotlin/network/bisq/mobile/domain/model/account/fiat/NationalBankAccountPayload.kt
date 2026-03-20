package network.bisq.mobile.domain.model.account.fiat

data class NationalBankAccountPayload(
    val countryCode: String,
    val selectedCurrencyCode: String,
    val holderName: String? = null,
    val holderId: String? = null,
    val bankName: String? = null,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountType? = null,
    val nationalAccountId: String? = null,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        BankAccountPayloadValidation.validateCommon(
            countryCode,
            selectedCurrencyCode,
            holderName,
            holderId,
            bankName,
            bankId,
            branchId,
            accountNr,
            nationalAccountId,
        )
    }
}
