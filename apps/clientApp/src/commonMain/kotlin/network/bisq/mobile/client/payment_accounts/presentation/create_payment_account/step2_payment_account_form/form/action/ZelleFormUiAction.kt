package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action

sealed interface ZelleFormUiAction : AccountFormUiAction {
    data class OnHolderNameChange(
        val value: String,
    ) : ZelleFormUiAction

    data class OnEmailOrMobileNrChange(
        val value: String,
    ) : ZelleFormUiAction
}
