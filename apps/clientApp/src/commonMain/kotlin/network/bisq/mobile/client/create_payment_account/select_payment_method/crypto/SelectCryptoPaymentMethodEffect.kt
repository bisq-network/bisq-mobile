package network.bisq.mobile.client.create_payment_account.select_payment_method.crypto

import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod

sealed interface SelectCryptoPaymentMethodEffect {
    data class NavigateToNextScreen(
        val selectedPaymentMethod: CryptoPaymentMethod,
    ) : SelectCryptoPaymentMethodEffect
}
