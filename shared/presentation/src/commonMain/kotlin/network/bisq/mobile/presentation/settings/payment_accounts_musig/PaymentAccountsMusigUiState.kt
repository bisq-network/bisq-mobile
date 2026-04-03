package network.bisq.mobile.presentation.settings.payment_accounts_musig

import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.CryptoAccountVO
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.FiatAccountVO

data class PaymentAccountsMusigUiState(
    val fiatAccounts: List<FiatAccountVO> = emptyList(),
    val cryptoAccounts: List<CryptoAccountVO> = emptyList(),
    val isLoadingAccounts: Boolean = false,
    val isLoadingAccountsError: Boolean = false,
    val selectedTab: PaymentAccountTab = PaymentAccountTab.FIAT,
    val showDeleteConfirmationDialog: Boolean = false,
)
