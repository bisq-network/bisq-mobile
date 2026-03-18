package network.bisq.mobile.presentation.settings.payment_accounts

import network.bisq.mobile.data.replicated.api.dto.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class PaymentAccountsUiState(
    val accounts: List<UserDefinedFiatAccountDto> = emptyList(),
    val selectedAccountIndex: Int = -1,
    val isLoadingAccounts: Boolean = false,
    val isLoadingAccountsError: Boolean = false,
    val accountNameEntry: DataEntry = DataEntry(),
    val accountDescriptionEntry: DataEntry = DataEntry(),
    val showDeleteConfirmationDialog: Boolean = false,
    val showEditAccountState: Boolean = false,
    val showAddAccountState: Boolean = false,
)
