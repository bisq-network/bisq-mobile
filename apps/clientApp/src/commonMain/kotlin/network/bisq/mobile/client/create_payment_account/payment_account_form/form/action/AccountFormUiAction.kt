package network.bisq.mobile.client.create_payment_account.payment_account_form.form.action

sealed interface AccountFormUiAction {
    data class OnUniqueAccountNameChange(
        val value: String,
    ) : AccountFormUiAction

    data object OnNextClick : AccountFormUiAction
}
