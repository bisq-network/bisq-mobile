package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class WiseUsdAccountPayload(
    val countryCode: String,
    val holderName: String,
    val email: String,
    val beneficiaryAddress: String,
) : FiatAccountPayload {
    companion object {
        const val BENEFICIARY_ADDRESS_MIN_LENGTH = 5
        const val BENEFICIARY_ADDRESS_MAX_LENGTH = 200
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        require(EmailValidation.isValid(email)) { "Email is invalid" }
        PaymentAccountValidation.validateHolderName(holderName)
        NetworkDataValidation.validateRequiredText(
            beneficiaryAddress,
            BENEFICIARY_ADDRESS_MIN_LENGTH,
            BENEFICIARY_ADDRESS_MAX_LENGTH,
        )
    }
}
