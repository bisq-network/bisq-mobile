package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.replicated.common.validation.PhoneNumberValidation

data class ZelleAccountPayload(
    val holderName: String,
    val emailOrMobileNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode("US")
        PaymentAccountValidation.validateHolderName(holderName)
        require(
            EmailValidation.isValid(emailOrMobileNr) ||
                PhoneNumberValidation.isValid(emailOrMobileNr, "US"),
        ) {
            "Email or mobile number is invalid"
        }
    }
}
