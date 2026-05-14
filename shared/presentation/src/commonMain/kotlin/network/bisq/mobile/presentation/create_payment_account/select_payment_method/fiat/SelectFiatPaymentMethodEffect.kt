package network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod

sealed interface SelectFiatPaymentMethodEffect {
    data class NavigateToNextScreen(
        val selectedPaymentMethod: FiatPaymentMethod,
    ) : SelectFiatPaymentMethodEffect
}
