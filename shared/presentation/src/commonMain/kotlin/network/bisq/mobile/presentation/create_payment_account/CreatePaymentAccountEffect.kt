package network.bisq.mobile.presentation.create_payment_account

sealed interface CreatePaymentAccountEffect {
    data object NavigateToPaymentAccountForm : CreatePaymentAccountEffect

    data object NavigateToPaymentAccountReview : CreatePaymentAccountEffect
}
