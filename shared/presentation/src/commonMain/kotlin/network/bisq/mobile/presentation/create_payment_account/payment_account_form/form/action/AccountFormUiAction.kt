package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action

sealed interface AccountFormUiAction {
    data class OnUniqueAccountNameChange(
        val value: String,
    ) : AccountFormUiAction

    data object OnNextClick : AccountFormUiAction
}
