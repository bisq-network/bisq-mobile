package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PhoneNumberValidation

data class BizumAccountPayload(
    val countryCode: String,
    val mobileNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        require(countryCode == "ES") { "Country code must be 'ES' for Bizum accounts" }
        require(PhoneNumberValidation.isValid(mobileNr, "ES")) { "Mobile number is invalid" }
    }
}
