package network.bisq.mobile.presentation.peer_profile

sealed interface PeerProfileUiAction {
    data object OnRetryLoadClick : PeerProfileUiAction

    data object OnSendPrivateMessageClick : PeerProfileUiAction

    data object OnIgnoreClick : PeerProfileUiAction

    data object OnConfirmIgnore : PeerProfileUiAction

    data object OnDismissIgnoreDialog : PeerProfileUiAction

    data object OnUndoIgnoreClick : PeerProfileUiAction

    data object OnReportClick : PeerProfileUiAction

    data object OnReportSuccess : PeerProfileUiAction

    /**
     * @param reportMessage what the user had typed, kept so the dialog can be reopened with it.
     *   `ReportUserPresenter` has already surfaced the error itself.
     */
    data class OnReportFailure(
        val reportMessage: String,
    ) : PeerProfileUiAction
}
