package network.bisq.mobile.presentation.create_payment_account.account_review

sealed interface PaymentAccountReviewEffect {
    data object CloseCreateAccountFlow : PaymentAccountReviewEffect
}
