package network.bisq.mobile.presentation.create_payment_account.select_payment_method.crypto

import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO

data class SelectCryptoPaymentMethodUiState(
    val paymentMethods: List<CryptoPaymentMethodVO> = emptyList(),
    val selectedPaymentMethod: CryptoPaymentMethodVO? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val searchQuery: String = EMPTY_STRING,
)
