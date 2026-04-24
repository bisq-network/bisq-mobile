package network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat

import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO

sealed interface SelectFiatPaymentMethodEffect {
    data class NavigateToNextScreen(
        val selectedPaymentMethod: FiatPaymentMethodVO,
    ) : SelectFiatPaymentMethodEffect
}
