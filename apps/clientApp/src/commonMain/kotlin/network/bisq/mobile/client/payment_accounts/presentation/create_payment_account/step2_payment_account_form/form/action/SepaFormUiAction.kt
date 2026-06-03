package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action

sealed interface SepaFormUiAction : AccountFormUiAction {
    data class OnCountrySelect(
        val index: Int,
    ) : SepaFormUiAction

    data class OnHolderNameChange(
        val value: String,
    ) : SepaFormUiAction

    data class OnIbanChange(
        val value: String,
    ) : SepaFormUiAction

    data class OnBicChange(
        val value: String,
    ) : SepaFormUiAction

    data object OnOpenAcceptedCountriesPicker : SepaFormUiAction

    data object OnCloseAcceptedCountriesPicker : SepaFormUiAction

    data class OnAcceptedCountrySearchChange(
        val value: String,
    ) : SepaFormUiAction

    data class OnAcceptedCountryToggle(
        val code: String,
    ) : SepaFormUiAction

    data object OnSelectAllAcceptedCountries : SepaFormUiAction

    data object OnClearAllAcceptedCountries : SepaFormUiAction
}
