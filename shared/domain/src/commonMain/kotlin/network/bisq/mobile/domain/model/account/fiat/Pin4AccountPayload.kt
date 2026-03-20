package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PhoneNumberValidation

data class Pin4AccountPayload(
    val mobileNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode("PL")
        require(PhoneNumberValidation.isValid(mobileNr, "PL")) { "Mobile number is invalid" }
    }
}
