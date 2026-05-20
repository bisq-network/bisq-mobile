package network.bisq.mobile.client.create_payment_account

sealed interface CreatePaymentAccountEffect {
    data object NavigateToPaymentAccountForm : CreatePaymentAccountEffect

    data object NavigateToPaymentAccountReview : CreatePaymentAccountEffect
}
