package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

sealed interface WebLinkConfirmationUiAction {
    data class OnDontShowAgainChange(
        val checked: Boolean,
    ) : WebLinkConfirmationUiAction

    data object OnConfirm : WebLinkConfirmationUiAction

    data class OnDismiss(
        val toCopy: Boolean,
    ) : WebLinkConfirmationUiAction
}
