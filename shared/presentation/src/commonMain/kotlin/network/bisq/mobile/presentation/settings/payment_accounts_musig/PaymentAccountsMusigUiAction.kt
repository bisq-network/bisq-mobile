package network.bisq.mobile.presentation.settings.payment_accounts_musig

sealed interface PaymentAccountsMusigUiAction {
    data object OnRetryLoadAccountsClick : PaymentAccountsMusigUiAction

    data object OnAddFiatAccountClick : PaymentAccountsMusigUiAction

    data object OnAddCryptoAccountClick : PaymentAccountsMusigUiAction

    data object OnDeleteAccountClick : PaymentAccountsMusigUiAction

    data object OnCancelDeleteAccountClick : PaymentAccountsMusigUiAction

    data object OnConfirmDeleteAccountClick : PaymentAccountsMusigUiAction

    data object OnSaveAccountClick : PaymentAccountsMusigUiAction

    data class OnAccountSelect(
        val index: Int,
    ) : PaymentAccountsMusigUiAction

    data object OnEditAccountClick : PaymentAccountsMusigUiAction

    data class OnTabSelect(
        val tab: PaymentAccountTab,
    ) : PaymentAccountsMusigUiAction
}
