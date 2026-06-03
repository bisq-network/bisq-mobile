package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise

sealed interface WiseFormUiAction {
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
