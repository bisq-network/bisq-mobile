package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut

sealed interface RevolutFormUiAction {
    data class OnUserNameChange(
        val value: String,
    ) : RevolutFormUiAction

    data object OnOpenCurrencyPicker : RevolutFormUiAction

    data object OnCloseCurrencyPicker : RevolutFormUiAction

    data class OnCurrencySearchChange(
        val value: String,
    ) : RevolutFormUiAction

    data class OnCurrencyToggle(
        val code: String,
    ) : RevolutFormUiAction

    data object OnSelectAllCurrencies : RevolutFormUiAction

    data object OnClearAllCurrencies : RevolutFormUiAction
}
