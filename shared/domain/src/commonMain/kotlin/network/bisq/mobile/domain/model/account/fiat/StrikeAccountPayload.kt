package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class StrikeAccountPayload(
    val countryCode: String,
    val holderName: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        NetworkDataValidation.validateRequiredText(holderName, 2, 70)
    }
}
