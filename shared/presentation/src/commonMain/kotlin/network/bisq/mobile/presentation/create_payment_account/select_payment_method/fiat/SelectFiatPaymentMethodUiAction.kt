package network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat

import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO

sealed interface SelectFiatPaymentMethodUiAction {
    data object OnRetryLoadPaymentMethodsClick : SelectFiatPaymentMethodUiAction

    data object OnNextClick : SelectFiatPaymentMethodUiAction

    data class OnSearchQueryChange(
        val query: String,
    ) : SelectFiatPaymentMethodUiAction

    data class OnRiskFilterChange(
        val riskFilter: FiatPaymentMethodChargebackRiskVO?,
    ) : SelectFiatPaymentMethodUiAction

    data class OnPaymentMethodClick(
        val paymentMethod: FiatPaymentMethodVO,
    ) : SelectFiatPaymentMethodUiAction
}
