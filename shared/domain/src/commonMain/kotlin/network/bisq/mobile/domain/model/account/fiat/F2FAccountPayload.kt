package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class F2FAccountPayload(
    val countryCode: String,
    val selectedCurrencyCode: String,
    val city: String,
    val contact: String,
    val extraInfo: String,
) : FiatAccountPayload {
    companion object {
        const val CITY_MIN_LENGTH = 2
        const val CITY_MAX_LENGTH = 50
        const val CONTACT_MIN_LENGTH = 2
        const val CONTACT_MAX_LENGTH = 100
        const val EXTRA_INFO_MAX_LENGTH = 300
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode)
        NetworkDataValidation.validateRequiredText(city, CITY_MIN_LENGTH, CITY_MAX_LENGTH)
        NetworkDataValidation.validateRequiredText(contact, CONTACT_MIN_LENGTH, CONTACT_MAX_LENGTH)
        if (extraInfo.isNotEmpty()) {
            NetworkDataValidation.validateText(extraInfo, EXTRA_INFO_MAX_LENGTH)
        }
    }
}
