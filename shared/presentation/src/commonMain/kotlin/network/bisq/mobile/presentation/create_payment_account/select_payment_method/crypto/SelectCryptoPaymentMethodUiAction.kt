package network.bisq.mobile.presentation.create_payment_account.select_payment_method.crypto

import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO

sealed interface SelectCryptoPaymentMethodUiAction {
    data object OnRetryLoadPaymentMethodsClick : SelectCryptoPaymentMethodUiAction

    data object OnNextClick : SelectCryptoPaymentMethodUiAction

    data class OnSearchQueryChange(
        val query: String,
    ) : SelectCryptoPaymentMethodUiAction

    data class OnPaymentMethodClick(
        val paymentMethod: CryptoPaymentMethodVO,
    ) : SelectCryptoPaymentMethodUiAction
}
