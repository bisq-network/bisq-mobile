package network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts

import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.presentation.ui.helpers.EMPTY_STRING

data class PaymentAccountsUiState(
    val accounts: List<UserDefinedFiatAccountVO> = emptyList(),
    val selectedAccountIndex: Int = -1,
    val isLoadingAccounts: Boolean = false,
    val isLoadingAccountsError: Boolean = false,
    val accountName: String = EMPTY_STRING,
    val accountNameInvalidMessage: String? = null,
    val accountDescription: String = EMPTY_STRING,
    val accountDescriptionInvalidMessage: String? = null,
    val showDeleteConfirmationDialog: Boolean = false,
    val showAddAccountBottomSheet: Boolean = false,
    val isEditAccountMode: Boolean = false,
)

sealed interface PaymentAccountsUiAction {
    data class OnAccountNameChange(val name: String) : PaymentAccountsUiAction
    data class OnAccountDescriptionChange(val description: String) : PaymentAccountsUiAction
    data object OnAddAccountClick : PaymentAccountsUiAction
    data object OnCancelAddAccountClick : PaymentAccountsUiAction
    data class OnConfirmAddAccountClick(val name: String, val description: String) :
        PaymentAccountsUiAction

    data object OnDeleteAccountClick : PaymentAccountsUiAction
    data object OnCancelDeleteAccountClick : PaymentAccountsUiAction
    data object OnConfirmDeleteAccountClick : PaymentAccountsUiAction
    data object OnSaveAccountClick : PaymentAccountsUiAction
    data object OnRetryLoadAccountsClick : PaymentAccountsUiAction
    data class OnAccountSelect(val index: Int) : PaymentAccountsUiAction
    data object OnEditAccountClick : PaymentAccountsUiAction
    data object OnCancelEditAccountClick : PaymentAccountsUiAction
}
