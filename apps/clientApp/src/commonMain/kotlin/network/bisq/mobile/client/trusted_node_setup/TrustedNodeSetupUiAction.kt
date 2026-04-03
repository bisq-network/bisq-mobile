package network.bisq.mobile.client.trusted_node_setup

import network.bisq.mobile.client.trusted_node_setup.components.SubscriptionsFailedDialogUiAction

sealed interface TrustedNodeSetupUiAction {
    data class OnPairingCodeChange(
        val value: String,
    ) : TrustedNodeSetupUiAction

    data object OnTestAndSavePress : TrustedNodeSetupUiAction

    data object OnCancelPress : TrustedNodeSetupUiAction

    data object OnShowQrCodeView : TrustedNodeSetupUiAction

    data object OnQrCodeViewDismiss : TrustedNodeSetupUiAction

    data object OnQrCodeFail : TrustedNodeSetupUiAction

    data object OnQrCodeErrorClose : TrustedNodeSetupUiAction

    data class OnQrCodeResult(
        val value: String,
    ) : TrustedNodeSetupUiAction

    data object OnPairWithNewNodePress : TrustedNodeSetupUiAction

    data object OnChangeNodeWarningConfirm : TrustedNodeSetupUiAction

    data object OnChangeNodeWarningCancel : TrustedNodeSetupUiAction

    data object OnConnectionFailedRetryPress : TrustedNodeSetupUiAction

    data object OnConnectionFailedPairWithNewNodePress : TrustedNodeSetupUiAction

    data object OnKeystoreErrorDismiss : TrustedNodeSetupUiAction

    data class OnSubscriptionsFailedDialogUiAction(
        val action: SubscriptionsFailedDialogUiAction,
    ) : TrustedNodeSetupUiAction
}
