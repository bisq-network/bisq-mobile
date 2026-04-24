package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action

sealed interface ZelleFormUiAction : AccountFormUiAction {
    data class OnHolderNameChange(
        val value: String,
    ) : ZelleFormUiAction

    data class OnEmailOrMobileNrChange(
        val value: String,
    ) : ZelleFormUiAction
}
