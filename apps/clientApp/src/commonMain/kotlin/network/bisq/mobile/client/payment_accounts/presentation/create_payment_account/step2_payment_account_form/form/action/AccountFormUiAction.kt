package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action

sealed interface AccountFormUiAction {
    data class OnUniqueAccountNameChange(
        val value: String,
    ) : AccountFormUiAction

    data object OnNextClick : AccountFormUiAction
}
