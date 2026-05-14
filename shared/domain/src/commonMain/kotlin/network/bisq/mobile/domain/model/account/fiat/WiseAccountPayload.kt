package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class WiseAccountPayload(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val email: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
) : FiatPaymentAccountPayload {
    init {
        verify()
    }

    fun verify() {
        require(selectedCurrencyCodes.isNotEmpty()) { "At least one currency code must be selected" }
        require(selectedCurrencyCodes.none { it.isBlank() }) { "Currency codes must not be blank" }
        require(paymentMethodName.isNotBlank()) { "Payment method name is required" }
        PaymentAccountValidation.validateHolderName(holderName)
        require(EmailValidation.isValid(email)) { "Email is invalid" }
    }
}
