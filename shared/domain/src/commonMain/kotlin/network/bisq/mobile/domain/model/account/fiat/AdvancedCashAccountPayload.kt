package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailUtil
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.replicated.common.validation.fiat.AdvancedCashAccountNrValidation

data class AdvancedCashAccountPayload(
    val selectedCurrencyCodes: List<String>,
    val accountNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateCurrencyCodes(
            selectedCurrencyCodes,
            FiatPaymentRailUtil.getAdvancedCashCurrencyCodes(),
            "Advanced Cash currency codes",
        )
        require(AdvancedCashAccountNrValidation.isValid(accountNr)) {
            "Account number is invalid"
        }
    }
}
