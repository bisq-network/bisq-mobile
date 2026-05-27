package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review

sealed interface PaymentAccountReviewEffect {
    data object CloseCreateAccountFlow : PaymentAccountReviewEffect
}
