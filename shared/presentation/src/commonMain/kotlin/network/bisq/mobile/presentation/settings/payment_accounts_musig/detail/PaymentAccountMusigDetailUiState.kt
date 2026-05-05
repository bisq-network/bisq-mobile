package network.bisq.mobile.presentation.settings.payment_accounts_musig.detail

import network.bisq.mobile.domain.model.account.PaymentAccount

data class PaymentAccountMusigDetailUiState(
    val paymentAccount: PaymentAccount? = null,
    val isAccountMissing: Boolean = false,
    val showDeleteConfirmationDialog: Boolean = false,
)
