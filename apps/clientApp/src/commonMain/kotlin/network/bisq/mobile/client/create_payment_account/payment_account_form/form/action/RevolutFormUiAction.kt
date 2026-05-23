package network.bisq.mobile.client.create_payment_account.payment_account_form.form.action

sealed interface RevolutFormUiAction : AccountFormUiAction {
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
