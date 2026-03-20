package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class USPostalMoneyOrderAccountPayload(
    val holderName: String,
    val postalAddress: String,
    val countryCode: String = "US",
) : FiatAccountPayload {
    companion object {
        const val POSTAL_ADDRESS_MIN_LENGTH = 5
        const val POSTAL_ADDRESS_MAX_LENGTH = 200
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        require(countryCode == "US") { "Country code must be 'US' for USPostalMoneyOrder accounts" }
        PaymentAccountValidation.validateHolderName(holderName)
        NetworkDataValidation.validateRequiredText(postalAddress, POSTAL_ADDRESS_MIN_LENGTH, POSTAL_ADDRESS_MAX_LENGTH)
    }
}
