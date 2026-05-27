package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.fiat

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod

sealed interface SelectFiatPaymentMethodEffect {
    data class NavigateToNextScreen(
        val selectedPaymentMethod: FiatPaymentMethod,
    ) : SelectFiatPaymentMethodEffect
}
