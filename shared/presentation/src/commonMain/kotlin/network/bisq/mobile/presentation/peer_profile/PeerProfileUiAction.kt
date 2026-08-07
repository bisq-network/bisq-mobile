package network.bisq.mobile.presentation.peer_profile

sealed interface PeerProfileUiAction {
    data object OnRetryLoadClick : PeerProfileUiAction

    data object OnIgnoreClick : PeerProfileUiAction

    data object OnConfirmIgnore : PeerProfileUiAction

    data object OnDismissIgnoreDialog : PeerProfileUiAction

    data object OnUndoIgnoreClick : PeerProfileUiAction

    data object OnReportClick : PeerProfileUiAction

    data object OnDismissReportDialog : PeerProfileUiAction

    /**
     * @param message the error to surface.
     * @param reportMessage what the user had typed, kept so the dialog can be reopened with it.
     */
    data class OnReportFailure(
        val message: String,
        val reportMessage: String,
    ) : PeerProfileUiAction
}
