package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review

import network.bisq.mobile.domain.model.account.PaymentAccount

data class PaymentAccountReviewUiState(
    val isLoading: Boolean = true,
    val paymentAccount: PaymentAccount? = null,
)
