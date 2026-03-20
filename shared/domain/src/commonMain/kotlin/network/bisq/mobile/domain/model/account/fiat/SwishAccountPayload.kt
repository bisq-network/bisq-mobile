package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.replicated.common.validation.PhoneNumberValidation

data class SwishAccountPayload(
    val holderName: String,
    val mobileNr: String,
    val countryCode: String = "SE",
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        require(countryCode == "SE") { "Country code must be 'SE' for Swish accounts" }
        PaymentAccountValidation.validateHolderName(holderName)
        require(PhoneNumberValidation.isValid(mobileNr, "SE")) { "Mobile number is invalid" }
    }
}
