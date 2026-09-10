package network.bisq.mobile.presentation.peer_profile

sealed interface PeerProfileUiAction {
    data object OnRetryLoadClick : PeerProfileUiAction

    data object OnSendPrivateMessageClick : PeerProfileUiAction

    data object OnIgnoreClick : PeerProfileUiAction

    data object OnConfirmIgnore : PeerProfileUiAction

    data object OnDismissIgnoreDialog : PeerProfileUiAction

    data object OnUndoIgnoreClick : PeerProfileUiAction

    data object OnAddContactClick : PeerProfileUiAction

    data object OnEditContactDetailsClick : PeerProfileUiAction

    data object OnDismissEditContactDetailsDialog : PeerProfileUiAction

    data class OnContactTagChanged(
        val tag: String,
    ) : PeerProfileUiAction

    data class OnContactNotesChanged(
        val notes: String,
    ) : PeerProfileUiAction

    data class OnContactTrustScoreChanged(
        val trustScore: Double,
    ) : PeerProfileUiAction

    data object OnSaveContactDetailsClick : PeerProfileUiAction

    data object OnRemoveContactClick : PeerProfileUiAction

    /**
     * A tap on one of the peer's offers in the "Trade again" section. Every row funnels through the
     * same eligibility gate: an eligible offer goes straight into the take-offer wizard, an
     * ineligible one opens the reputation-requirement dialog.
     */
    data class OnPeerOfferClick(
        val offerId: String,
    ) : PeerProfileUiAction

    data object OnDismissNotEnoughReputationDialog : PeerProfileUiAction

    /** The "View all {N} offers" affordance under the capped inline list. */
    data object OnViewAllOffersClick : PeerProfileUiAction

    /** Confirm on the seller-as-taker variant of the reputation dialog: build my own reputation. */
    data object OnNavigateToReputationClick : PeerProfileUiAction

    /** Confirm on the buyer variant: open the reputation wiki explaining the maker's requirement. */
    data object OnOpenReputationWikiClick : PeerProfileUiAction

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
