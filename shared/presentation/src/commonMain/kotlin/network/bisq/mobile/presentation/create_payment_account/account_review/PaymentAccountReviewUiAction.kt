package network.bisq.mobile.presentation.create_payment_account.account_review

import network.bisq.mobile.domain.model.account.PaymentAccount

sealed interface PaymentAccountReviewUiAction {
    data class OnCreateAccountClick(
        val account: PaymentAccount,
    ) : PaymentAccountReviewUiAction
}
