package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto

sealed interface CryptoAccountFormUiAction {
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
