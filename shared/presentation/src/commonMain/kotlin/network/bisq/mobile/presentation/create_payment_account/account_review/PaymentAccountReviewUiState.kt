package network.bisq.mobile.presentation.create_payment_account.account_review

import network.bisq.mobile.domain.model.account.PaymentAccount

data class PaymentAccountReviewUiState(
    val isLoading: Boolean = true,
    val paymentAccount: PaymentAccount? = null,
)
