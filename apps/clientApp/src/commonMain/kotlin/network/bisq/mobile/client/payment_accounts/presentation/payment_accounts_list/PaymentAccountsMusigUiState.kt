package network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list

import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.model.CryptoAccountVO
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.model.FiatAccountVO

data class PaymentAccountsMusigUiState(
    val fiatAccounts: List<FiatAccountVO> = emptyList(),
    val cryptoAccounts: List<CryptoAccountVO> = emptyList(),
    val isLoadingAccounts: Boolean = false,
    val isLoadingAccountsError: Boolean = false,
    val selectedTab: PaymentAccountTab = PaymentAccountTab.FIAT,
    val showDeleteConfirmationDialog: Boolean = false,
)
