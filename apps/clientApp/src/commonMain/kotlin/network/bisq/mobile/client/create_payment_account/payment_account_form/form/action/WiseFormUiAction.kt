package network.bisq.mobile.client.create_payment_account.payment_account_form.form.action

sealed interface WiseFormUiAction : AccountFormUiAction {
    data class OnHolderNameChange(
        val value: String,
    ) : WiseFormUiAction

    data class OnEmailChange(
        val value: String,
    ) : WiseFormUiAction

    data object OnOpenCurrencyPicker : WiseFormUiAction

    data object OnCloseCurrencyPicker : WiseFormUiAction

    data class OnCurrencySearchChange(
        val value: String,
    ) : WiseFormUiAction

    data class OnCurrencyToggle(
        val code: String,
    ) : WiseFormUiAction

    data object OnSelectAllCurrencies : WiseFormUiAction

    data object OnClearAllCurrencies : WiseFormUiAction
}
