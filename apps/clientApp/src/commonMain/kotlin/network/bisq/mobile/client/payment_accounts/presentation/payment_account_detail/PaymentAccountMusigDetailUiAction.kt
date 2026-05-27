package network.bisq.mobile.client.payment_accounts.presentation.payment_account_detail

sealed interface PaymentAccountMusigDetailUiAction {
    data object OnDeleteAccountClick : PaymentAccountMusigDetailUiAction

    data object OnConfirmDeleteAccountClick : PaymentAccountMusigDetailUiAction

    data object OnCancelDeleteAccountClick : PaymentAccountMusigDetailUiAction
}
