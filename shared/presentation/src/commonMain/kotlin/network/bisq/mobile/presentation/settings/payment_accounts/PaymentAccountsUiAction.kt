package network.bisq.mobile.presentation.settings.payment_accounts

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
