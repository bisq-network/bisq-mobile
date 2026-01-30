package network.bisq.mobile.presentation.settings.payment_accounts

import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class PaymentAccountsUiState(
    val accounts: List<UserDefinedFiatAccountVO> = emptyList(),
    val selectedAccountIndex: Int = -1,
    val isLoadingAccounts: Boolean = false,
    val isLoadingAccountsError: Boolean = false,
    val accountNameEntry: DataEntry = DataEntry(),
    val accountDescriptionEntry: DataEntry = DataEntry(),
    val showDeleteConfirmationDialog: Boolean = false,
    val showEditAccountState: Boolean = false,
    val showAddAccountState: Boolean = false,
)
