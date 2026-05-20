package network.bisq.mobile.client.create_payment_account.select_payment_method.crypto

import network.bisq.mobile.client.common.presentation.model.account.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

data class SelectCryptoPaymentMethodUiState(
    val paymentMethods: List<CryptoPaymentMethodVO> = emptyList(),
    val selectedPaymentMethod: CryptoPaymentMethodVO? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val searchQuery: String = EMPTY_STRING,
)
