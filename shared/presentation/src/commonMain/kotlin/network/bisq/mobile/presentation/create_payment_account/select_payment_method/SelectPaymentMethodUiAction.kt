package network.bisq.mobile.presentation.create_payment_account.select_payment_method

import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO

sealed interface SelectPaymentMethodUiAction {
    data object OnRetryLoadPaymentMethodsClick : SelectPaymentMethodUiAction

    data class OnSearchQueryChange(
        val query: String,
    ) : SelectPaymentMethodUiAction

    data class OnRiskFilterChange(
        val riskFilter: FiatPaymentMethodChargebackRiskVO?,
    ) : SelectPaymentMethodUiAction

    data class OnFiatPaymentMethodClick(
        val paymentMethod: FiatPaymentMethodVO,
    ) : SelectPaymentMethodUiAction

    data class OnCryptoPaymentMethodClick(
        val paymentMethod: CryptoPaymentMethodVO,
    ) : SelectPaymentMethodUiAction
}
