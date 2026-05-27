package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account

sealed interface CreatePaymentAccountEffect {
    data object NavigateToPaymentAccountForm : CreatePaymentAccountEffect

    data object NavigateToPaymentAccountReview : CreatePaymentAccountEffect
}
