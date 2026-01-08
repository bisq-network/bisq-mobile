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

sealed interface PaymentAccountsUiAction {
    data class OnAccountNameChange(
        val name: String,
    ) : PaymentAccountsUiAction

    data class OnAccountDescriptionChange(
        val description: String,
    ) : PaymentAccountsUiAction

    data object OnRetryLoadAccountsClick : PaymentAccountsUiAction

    data object OnAddAccountClick : PaymentAccountsUiAction

    data object OnConfirmAddAccountClick : PaymentAccountsUiAction

    data object OnDeleteAccountClick : PaymentAccountsUiAction

    data object OnCancelDeleteAccountClick : PaymentAccountsUiAction

    data object OnConfirmDeleteAccountClick : PaymentAccountsUiAction

    data object OnSaveAccountClick : PaymentAccountsUiAction

    data class OnAccountSelect(
        val index: Int,
    ) : PaymentAccountsUiAction

    data object OnEditAccountClick : PaymentAccountsUiAction

    data object OnCancelAddEditAccountClick : PaymentAccountsUiAction
}
