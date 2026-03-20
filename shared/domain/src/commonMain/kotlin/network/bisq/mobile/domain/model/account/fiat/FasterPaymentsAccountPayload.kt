package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class FasterPaymentsAccountPayload(
    val holderName: String,
    val sortCode: String,
    val accountNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode("GB")
        PaymentAccountValidation.validateHolderName(holderName)
        PaymentAccountValidation.validateFasterPaymentsSortCode(sortCode)
        PaymentAccountValidation.validateFasterPaymentsAccountNr(accountNr)
    }
}
