package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailUtil
import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class PayseraAccountPayload(
    val selectedCurrencyCodes: List<String>,
    val email: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateCurrencyCodes(
            selectedCurrencyCodes,
            FiatPaymentRailUtil.getPayseraCurrencyCodes(),
            "Paysera currency codes",
        )
        require(EmailValidation.isValid(email)) { "Email is invalid" }
    }
}
