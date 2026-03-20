package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class CashByMailAccountPayload(
    val selectedCurrencyCode: String,
    val postalAddress: String,
    val contact: String,
    val extraInfo: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode)
    }
}
