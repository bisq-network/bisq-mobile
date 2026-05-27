package network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list

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
