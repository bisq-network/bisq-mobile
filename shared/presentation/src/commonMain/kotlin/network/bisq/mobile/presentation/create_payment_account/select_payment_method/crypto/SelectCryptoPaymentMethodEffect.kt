package network.bisq.mobile.presentation.create_payment_account.select_payment_method.crypto

import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO

sealed interface SelectCryptoPaymentMethodEffect {
    data class NavigateToNextScreen(
        val selectedPaymentMethod: CryptoPaymentMethodVO,
    ) : SelectCryptoPaymentMethodEffect
}
