package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class UpiAccountPayload(
    val virtualPaymentAddress: String,
    val countryCode: String = "IN",
) : FiatAccountPayload {
    companion object {
        const val VPA_MIN_LENGTH = 2
        const val VPA_MAX_LENGTH = 100
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        require(countryCode == "IN") { "Country code must be 'IN' for Upi accounts" }
        NetworkDataValidation.validateRequiredText(virtualPaymentAddress, VPA_MIN_LENGTH, VPA_MAX_LENGTH)
    }
}
