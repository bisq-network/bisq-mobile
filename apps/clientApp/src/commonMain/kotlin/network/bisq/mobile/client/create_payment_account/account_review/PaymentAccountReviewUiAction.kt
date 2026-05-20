package network.bisq.mobile.client.create_payment_account.account_review

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

sealed interface PaymentAccountReviewUiAction {
    data class OnCreateAccountClick(
        val account: CreatePaymentAccount,
    ) : PaymentAccountReviewUiAction
}
