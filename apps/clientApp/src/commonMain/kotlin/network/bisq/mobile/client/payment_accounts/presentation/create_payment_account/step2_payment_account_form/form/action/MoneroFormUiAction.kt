package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action

sealed interface MoneroFormUiAction : CryptoAccountFormUiAction {
    data class OnUseSubAddressesChange(
        val value: Boolean,
    ) : MoneroFormUiAction

    data class OnMainAddressChange(
        val value: String,
    ) : MoneroFormUiAction

    data class OnPrivateViewKeyChange(
        val value: String,
    ) : MoneroFormUiAction

    data class OnAccountIndexChange(
        val value: String,
    ) : MoneroFormUiAction

    data class OnInitialSubAddressIndexChange(
        val value: String,
    ) : MoneroFormUiAction
}
