package network.bisq.mobile.client.create_payment_account

import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

sealed interface CreatePaymentAccountUiAction {
    data class OnNavigateFromSelectPaymentMethod(
        val paymentMethod: PaymentMethod,
    ) : CreatePaymentAccountUiAction

    data class OnNavigateFromPaymentAccountForm(
        val createPaymentAccount: CreatePaymentAccount,
    ) : CreatePaymentAccountUiAction
}
