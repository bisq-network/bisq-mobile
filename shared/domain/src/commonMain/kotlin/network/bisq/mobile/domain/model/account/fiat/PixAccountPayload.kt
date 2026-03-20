package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class PixAccountPayload(
    val holderName: String,
    val pixKey: String,
    val countryCode: String,
) : FiatAccountPayload {
    companion object {
        const val PIX_KEY_MIN_LENGTH = 2
        const val PIX_KEY_MAX_LENGTH = 100
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        PaymentAccountValidation.validateHolderName(holderName)
        NetworkDataValidation.validateRequiredText(pixKey, PIX_KEY_MIN_LENGTH, PIX_KEY_MAX_LENGTH)
    }
}
