package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

sealed interface PaymentAccountReviewUiAction {
    data class OnCreateAccountClick(
        val account: CreatePaymentAccount,
    ) : PaymentAccountReviewUiAction
}
