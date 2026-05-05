package network.bisq.mobile.presentation.settings.payment_accounts_musig

sealed interface PaymentAccountsMusigUiAction {
    data object OnRetryLoadAccountsClick : PaymentAccountsMusigUiAction

    data object OnAddFiatAccountClick : PaymentAccountsMusigUiAction

    data object OnAddCryptoAccountClick : PaymentAccountsMusigUiAction

    data class OnAccountClick(
        val index: Int,
    ) : PaymentAccountsMusigUiAction

    data class OnTabSelect(
        val tab: PaymentAccountTab,
    ) : PaymentAccountsMusigUiAction
}
