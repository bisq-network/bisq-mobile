package network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat

import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO

data class SelectFiatPaymentMethodUiState(
    val paymentMethods: List<FiatPaymentMethodVO> = emptyList(),
    val selectedPaymentMethod: FiatPaymentMethodVO? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val searchQuery: String = EMPTY_STRING,
    val activeRiskFilter: FiatPaymentMethodChargebackRiskVO? = null,
)
