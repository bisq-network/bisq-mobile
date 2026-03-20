package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class SwiftAccountPayload(
    val bankCountryCode: String,
    val beneficiaryName: String,
    val beneficiaryAccountNr: String,
    val beneficiaryPhone: String? = null,
    val beneficiaryAddress: String,
    val selectedCurrencyCode: String,
    val bankSwiftCode: String,
    val bankName: String,
    val bankBranch: String? = null,
    val bankAddress: String,
    val intermediaryBankCountryCode: String? = null,
    val intermediaryBankSwiftCode: String? = null,
    val intermediaryBankName: String? = null,
    val intermediaryBankBranch: String? = null,
    val intermediaryBankAddress: String? = null,
    val additionalInstructions: String? = null,
) : FiatAccountPayload {
    companion object {
        const val NAME_MIN_LENGTH = 2
        const val NAME_MAX_LENGTH = 100
        const val ACCOUNT_NR_MIN_LENGTH = 2
        const val ACCOUNT_NR_MAX_LENGTH = 50
        const val ADDRESS_MIN_LENGTH = 5
        const val ADDRESS_MAX_LENGTH = 200
        const val SWIFT_CODE_MIN_LENGTH = 8
        const val SWIFT_CODE_MAX_LENGTH = 11
        const val PHONE_MIN_LENGTH = 5
        const val PHONE_MAX_LENGTH = 30
        const val INSTRUCTIONS_MIN_LENGTH = 2
        const val INSTRUCTIONS_MAX_LENGTH = 300
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(bankCountryCode)
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode)
        NetworkDataValidation.validateRequiredText(beneficiaryName, NAME_MIN_LENGTH, NAME_MAX_LENGTH)
        NetworkDataValidation.validateRequiredText(beneficiaryAccountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH)
        NetworkDataValidation.validateRequiredText(beneficiaryAddress, ADDRESS_MIN_LENGTH, ADDRESS_MAX_LENGTH)
        NetworkDataValidation.validateRequiredText(bankSwiftCode, SWIFT_CODE_MIN_LENGTH, SWIFT_CODE_MAX_LENGTH)
        NetworkDataValidation.validateRequiredText(bankName, NAME_MIN_LENGTH, NAME_MAX_LENGTH)
        NetworkDataValidation.validateRequiredText(bankAddress, ADDRESS_MIN_LENGTH, ADDRESS_MAX_LENGTH)

        bankBranch?.let { NetworkDataValidation.validateRequiredText(it, NAME_MIN_LENGTH, NAME_MAX_LENGTH) }
        intermediaryBankCountryCode?.let { NetworkDataValidation.validateCode(it) }
        intermediaryBankSwiftCode?.let { NetworkDataValidation.validateRequiredText(it, SWIFT_CODE_MIN_LENGTH, SWIFT_CODE_MAX_LENGTH) }
        intermediaryBankName?.let { NetworkDataValidation.validateRequiredText(it, NAME_MIN_LENGTH, NAME_MAX_LENGTH) }
        intermediaryBankBranch?.let { NetworkDataValidation.validateRequiredText(it, NAME_MIN_LENGTH, NAME_MAX_LENGTH) }
        intermediaryBankAddress?.let { NetworkDataValidation.validateRequiredText(it, ADDRESS_MIN_LENGTH, ADDRESS_MAX_LENGTH) }
        beneficiaryPhone?.let { NetworkDataValidation.validateRequiredText(it, PHONE_MIN_LENGTH, PHONE_MAX_LENGTH) }
        additionalInstructions?.let { NetworkDataValidation.validateRequiredText(it, INSTRUCTIONS_MIN_LENGTH, INSTRUCTIONS_MAX_LENGTH) }
    }
}
