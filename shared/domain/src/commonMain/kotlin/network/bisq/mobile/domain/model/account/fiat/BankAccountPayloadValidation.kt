package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

object BankAccountPayloadValidation {
    const val HOLDER_ID_MIN_LENGTH = 2
    const val HOLDER_ID_MAX_LENGTH = 50
    const val BANK_NAME_MIN_LENGTH = 2
    const val BANK_NAME_MAX_LENGTH = 70
    const val BANK_ID_MIN_LENGTH = 1
    const val BANK_ID_MAX_LENGTH = 50
    const val BRANCH_ID_MIN_LENGTH = 1
    const val BRANCH_ID_MAX_LENGTH = 50
    const val ACCOUNT_NR_MIN_LENGTH = 1
    const val ACCOUNT_NR_MAX_LENGTH = 50
    const val NATIONAL_ACCOUNT_ID_MIN_LENGTH = 1
    const val NATIONAL_ACCOUNT_ID_MAX_LENGTH = 50

    fun validateCommon(
        countryCode: String,
        selectedCurrencyCode: String,
        holderName: String?,
        holderId: String?,
        bankName: String?,
        bankId: String?,
        branchId: String?,
        accountNr: String,
        nationalAccountId: String?,
    ) {
        NetworkDataValidation.validateCode(countryCode)
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode)
        holderName?.let(PaymentAccountValidation::validateHolderName)
        holderId?.let { NetworkDataValidation.validateRequiredText(it, HOLDER_ID_MIN_LENGTH, HOLDER_ID_MAX_LENGTH) }
        bankName?.let { NetworkDataValidation.validateRequiredText(it, BANK_NAME_MIN_LENGTH, BANK_NAME_MAX_LENGTH) }
        bankId?.let { NetworkDataValidation.validateRequiredText(it, BANK_ID_MIN_LENGTH, BANK_ID_MAX_LENGTH) }
        branchId?.let { NetworkDataValidation.validateRequiredText(it, BRANCH_ID_MIN_LENGTH, BRANCH_ID_MAX_LENGTH) }
        NetworkDataValidation.validateRequiredText(accountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH)
        nationalAccountId?.let {
            NetworkDataValidation.validateRequiredText(it, NATIONAL_ACCOUNT_ID_MIN_LENGTH, NATIONAL_ACCOUNT_ID_MAX_LENGTH)
        }
    }
}
