package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account

import network.bisq.mobile.client.payment_accounts.domain.model.PaymentMethod
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

sealed interface CreatePaymentAccountUiAction {
    data class OnNavigateFromSelectPaymentMethod(
        val paymentMethod: PaymentMethod,
    ) : CreatePaymentAccountUiAction

    data class OnNavigateFromPaymentAccountForm(
        val createPaymentAccount: CreatePaymentAccount,
    ) : CreatePaymentAccountUiAction
}
