package network.bisq.mobile.client.create_payment_account.select_payment_method.fiat

import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodVO

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
