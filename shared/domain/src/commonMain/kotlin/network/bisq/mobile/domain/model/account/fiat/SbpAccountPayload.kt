package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.replicated.common.validation.PhoneNumberValidation

data class SbpAccountPayload(
    val holderName: String,
    val mobileNumber: String,
    val bankName: String,
) : FiatAccountPayload {
    companion object {
        const val BANK_NAME_MIN_LENGTH = 2
        const val BANK_NAME_MAX_LENGTH = 70
    }

    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateHolderName(holderName)
        NetworkDataValidation.validateRequiredText(bankName, BANK_NAME_MIN_LENGTH, BANK_NAME_MAX_LENGTH)
        require(PhoneNumberValidation.isValid(mobileNumber, "RU")) { "Mobile number is invalid" }
    }
}
