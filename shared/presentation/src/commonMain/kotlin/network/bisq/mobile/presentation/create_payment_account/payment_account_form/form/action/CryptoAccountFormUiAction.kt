package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action

sealed interface CryptoAccountFormUiAction : AccountFormUiAction {
    data class OnAddressChange(
        val value: String,
    ) : CryptoAccountFormUiAction

    data class OnIsInstantChange(
        val value: Boolean,
    ) : CryptoAccountFormUiAction

    data class OnIsAutoConfChange(
        val value: Boolean,
    ) : CryptoAccountFormUiAction

    data class OnAutoConfNumConfirmationsChange(
        val value: String,
    ) : CryptoAccountFormUiAction

    data class OnAutoConfMaxTradeAmountChange(
        val value: String,
    ) : CryptoAccountFormUiAction

    data class OnAutoConfExplorerUrlsChange(
        val value: String,
    ) : CryptoAccountFormUiAction
}
