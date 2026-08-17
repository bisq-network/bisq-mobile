package network.bisq.mobile.presentation.peer_profile

import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * State of the peer profile screen (issue #545).
 *
 * Note on i18n: the reputation score is held here as a raw scalar, not as a pre-built `UiString`.
 * It is rendered through a key that takes an argument, and a `UiString` built in the presenter would
 * freeze that argument at load time and go stale when the user switches language in-app. The
 * composable resolves it with `i18nText(key, arg)`, which re-remembers on `LocalLanguageCode`.
 *
 * "Traded with you N times" from the design spec is intentionally absent: no backend exposes a
 * per-peer trade count (`TradesServiceFacade.getClosedTradesPaginated` has no peer filter), and
 * counting only the in-memory open trades would under-report. Tracked as a follow-up.
 */
data class PeerProfileUiState(
    /** Kept for the avatar renderer and the report dialog, both of which need the full VO. */
    val userProfile: UserProfileVO? = null,
    val displayName: String = "",
    val starRating: Double = 0.0,
    val reputationScore: Long = 0L,
    val isReputationUnknown: Boolean = false,
    val isIgnored: Boolean = false,
    val isOwnProfile: Boolean = false,
    /**
     * False for own or ignored profiles, and on Bisq Connect when the paired node is too old to
     * advertise the private-chat capability. The button is then absent rather than disabled — a
     * permanently dead control reads worse than no control.
     */
    val canSendPrivateMessage: Boolean = false,
    val isOpeningPrivateChat: Boolean = false,
    val isLoading: Boolean = true,
    /** The peer is genuinely unknown to the network — terminal, no retry offered. */
    val isNotFound: Boolean = false,
    /**
     * The lookup itself failed. Kept separate from [isNotFound] because on the client flavour
     * `findUserProfile` is a round-trip to the trusted node, and a dropped connection must not be
     * reported to the user as "this peer does not exist".
     */
    val isLoadFailed: Boolean = false,
    val showIgnoreConfirmDialog: Boolean = false,
    val showReportDialog: Boolean = false,
    /**
     * Survives a failed report so reopening the dialog restores what the user typed, mirroring
     * `TradeChatPresenter.onReportUserError`. Null rather than empty on purpose: `ReportUserDialog`
     * only seeds its field for a non-null value, so null leaves an in-progress edit untouched if the
     * dialog recomposes.
     */
    val reportDraft: String? = null,
)
