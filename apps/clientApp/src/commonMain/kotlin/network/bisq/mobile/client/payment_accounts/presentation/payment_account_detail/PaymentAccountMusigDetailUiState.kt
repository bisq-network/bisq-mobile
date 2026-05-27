package network.bisq.mobile.client.payment_accounts.presentation.payment_account_detail

import network.bisq.mobile.domain.model.account.PaymentAccount

data class PaymentAccountMusigDetailUiState(
    val paymentAccount: PaymentAccount? = null,
    val isAccountMissing: Boolean = false,
    val showDeleteConfirmationDialog: Boolean = false,
)
