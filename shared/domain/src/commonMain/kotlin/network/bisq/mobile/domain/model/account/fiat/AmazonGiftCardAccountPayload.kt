package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class AmazonGiftCardAccountPayload(
    val countryCode: String,
    val selectedCurrencyCode: String,
    val emailOrMobileNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        require(selectedCurrencyCode.isNotBlank()) { "Selected currency code must not be blank" }
        require(emailOrMobileNr.isNotBlank()) { "Email or mobile number must not be blank" }
    }
}
