package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.crypto

import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod

sealed interface SelectCryptoPaymentMethodEffect {
    data class NavigateToNextScreen(
        val selectedPaymentMethod: CryptoPaymentMethod,
    ) : SelectCryptoPaymentMethodEffect
}
