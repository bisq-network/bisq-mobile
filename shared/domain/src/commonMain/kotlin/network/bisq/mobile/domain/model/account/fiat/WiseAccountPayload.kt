package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailUtil
import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class WiseAccountPayload(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val email: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateCurrencyCodes(
            selectedCurrencyCodes,
            FiatPaymentRailUtil.wiseCurrencies,
            "Wise currency codes",
        )
        require(EmailValidation.isValid(email)) { "Email is invalid" }
        PaymentAccountValidation.validateHolderName(holderName)
    }
}
