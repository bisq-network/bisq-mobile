package network.bisq.mobile.client.create_payment_account.select_payment_method.fiat

import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodVO
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

data class SelectFiatPaymentMethodUiState(
    val paymentMethods: List<FiatPaymentMethodVO> = emptyList(),
    val selectedPaymentMethod: FiatPaymentMethodVO? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val searchQuery: String = EMPTY_STRING,
    val activeRiskFilter: FiatPaymentMethodChargebackRiskVO? = null,
)
