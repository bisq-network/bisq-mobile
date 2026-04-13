package network.bisq.mobile.presentation.create_payment_account.select_payment_method

import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO

data class SelectPaymentMethodUiState(
    val fiatPaymentMethods: List<FiatPaymentMethodVO> = emptyList(),
    val cryptoPaymentMethods: List<CryptoPaymentMethodVO> = emptyList(),
    val selectedFiatPaymentMethod: FiatPaymentMethodVO? = null,
    val selectedCryptoPaymentMethod: CryptoPaymentMethodVO? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val searchQuery: String = EMPTY_STRING,
    val activeRiskFilter: FiatPaymentMethodChargebackRiskVO? = null,
)
