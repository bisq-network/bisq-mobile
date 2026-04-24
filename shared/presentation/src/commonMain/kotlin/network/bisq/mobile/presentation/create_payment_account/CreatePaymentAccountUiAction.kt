package network.bisq.mobile.presentation.create_payment_account

import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.PaymentMethodVO

sealed interface CreatePaymentAccountUiAction {
    data class OnNavigateFromSelectPaymentMethod(
        val paymentMethod: PaymentMethodVO,
    ) : CreatePaymentAccountUiAction

    data class OnNavigateFromPaymentAccountForm(
        val paymentAccount: PaymentAccount,
    ) : CreatePaymentAccountUiAction
}
