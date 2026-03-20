package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class MercadoPagoAccountPayload(
    val holderName: String,
    val holderId: String,
) : FiatAccountPayload {
    companion object {
        const val HOLDER_ID_MIN_LENGTH = 1
        const val HOLDER_ID_MAX_LENGTH = 50
    }

    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateHolderName(holderName)
        NetworkDataValidation.validateRequiredText(holderId, HOLDER_ID_MIN_LENGTH, HOLDER_ID_MAX_LENGTH)
    }
}
