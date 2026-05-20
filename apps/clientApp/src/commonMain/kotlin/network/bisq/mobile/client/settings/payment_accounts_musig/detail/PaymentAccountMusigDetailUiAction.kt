package network.bisq.mobile.client.settings.payment_accounts_musig.detail

sealed interface PaymentAccountMusigDetailUiAction {
    data object OnDeleteAccountClick : PaymentAccountMusigDetailUiAction

    data object OnConfirmDeleteAccountClick : PaymentAccountMusigDetailUiAction

    data object OnCancelDeleteAccountClick : PaymentAccountMusigDetailUiAction
}
