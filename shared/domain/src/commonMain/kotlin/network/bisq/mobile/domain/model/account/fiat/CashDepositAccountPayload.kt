package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class CashDepositAccountPayload(
    val countryCode: String,
    val selectedCurrencyCode: String,
    val holderName: String?,
    val holderId: String? = null,
    val bankName: String?,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountType? = null,
    val nationalAccountId: String? = null,
    val requirements: String? = null,
) : FiatAccountPayload {
    companion object {
        const val REQUIREMENTS_MAX_LENGTH = 150
    }

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
        NetworkDataValidation.validateText(requirements, REQUIREMENTS_MAX_LENGTH)
    }
}
