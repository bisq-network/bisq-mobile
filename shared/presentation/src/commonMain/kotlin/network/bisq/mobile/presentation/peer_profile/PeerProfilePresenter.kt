package network.bisq.mobile.presentation.peer_profile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatNotPermittedException
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

class PeerProfilePresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val privateChatServiceFacade: PrivateChatServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private companion object {
        /**
         * `ClientReputationServiceFacade.getReputation` returns a failure — not a zero score — for
         * any peer with no reputation yet, and only in release builds. Surfacing that as an error
         * would break the screen for exactly the peers a user most wants to inspect, so it is
         * mapped to this instead. See [loadReputation] for when a failure means zero and when it
         * means "not known yet".
         */
        val ZERO_REPUTATION = ReputationScoreVO(totalScore = 0L, fiveSystemScore = 0.0, ranking = 0)
    }

    private val _uiState = MutableStateFlow(PeerProfileUiState())
    val uiState: StateFlow<PeerProfileUiState> = _uiState.asStateFlow()

    private val _isIgnoreActionEnabled = MutableStateFlow(true)
    val isIgnoreActionEnabled: StateFlow<Boolean> = _isIgnoreActionEnabled.asStateFlow()

    private val _isOpenPrivateChatEnabled = MutableStateFlow(true)

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    /** The peer this presenter is bound to; null until [initialize]. */
    private var profileId: String? = null
    private var ignoredStateJob: Job? = null
    private var privateChatSupportJob: Job? = null

    /** Latest value of [PrivateChatServiceFacade.isSupported]; see [observePrivateChatSupport]. */
    private var isPrivateChatSupported: Boolean = false
    private var loadProfileJob: Job? = null
    private var reputationJob: Job? = null

    /**
     * Binds this presenter to one peer, once. The screen's `LaunchedEffect` re-fires whenever this
     * destination is revealed from the back stack, and reloading then would flash the loading state
     * over already-correct data — hence the early return.
     *
     * A second call for a *different* peer is ignored rather than handled: navigation gives every
     * destination its own back stack entry, so it also gives every peer its own presenter instance
     * (see `PresenterHolder` in `BackStackAwarePresenterLifecycleHelper`). The warning is there
     * because a caller that ever breaks that assumption would otherwise render the wrong peer in
     * silence.
     */
    fun initialize(profileId: String) {
        val bound = this.profileId
        if (bound != null) {
            if (bound != profileId) log.w { "Ignoring re-initialize with a different peer" }
            return
        }
        this.profileId = profileId
        loadProfile(profileId)
        observeIgnoredState(profileId)
        observePrivateChatSupport()
    }

    fun onAction(action: PeerProfileUiAction) {
        when (action) {
            PeerProfileUiAction.OnRetryLoadClick -> onRetryLoad()

            PeerProfileUiAction.OnSendPrivateMessageClick -> onSendPrivateMessage()

            PeerProfileUiAction.OnIgnoreClick ->
                _uiState.update { it.copy(showIgnoreConfirmDialog = true) }

            PeerProfileUiAction.OnConfirmIgnore -> onConfirmIgnore()

            PeerProfileUiAction.OnDismissIgnoreDialog ->
                _uiState.update { it.copy(showIgnoreConfirmDialog = false) }

            PeerProfileUiAction.OnUndoIgnoreClick -> onUndoIgnore()

            PeerProfileUiAction.OnReportClick ->
                _uiState.update { it.copy(showReportDialog = true) }

            PeerProfileUiAction.OnReportSuccess -> {
                _uiState.update { it.copy(showReportDialog = false, reportDraft = null) }
            }

            is PeerProfileUiAction.OnReportFailure -> onReportFailure(action.reportMessage)
        }
    }

    private fun onRetryLoad() {
        val profileId = this.profileId ?: return
        _uiState.update { it.copy(isLoading = true, isLoadFailed = false) }
        loadProfile(profileId)
    }

    /**
     * Only one load may be in flight. Retrying supersedes the previous attempt rather than racing
     * it: `findUserProfile` is a node round-trip on the client flavour, so a slow first attempt can
     * otherwise land *after* a fast retry and replace a rendered profile with its own failure.
     * Cancelling the superseded job is what prevents that — both attempts are for the same peer, so
     * nothing downstream could tell their writes apart.
     */
    private fun loadProfile(profileId: String) {
        loadProfileJob?.cancel()
        loadProfileJob =
            presenterScope.launch {
                try {
                    if (isOwnProfile(profileId)) {
                        _uiState.update { it.copy(isOwnProfile = true, isLoading = false) }
                        return@launch
                    }

                    val userProfile = userProfileServiceFacade.findUserProfile(profileId)
                    if (userProfile == null) {
                        _uiState.update { it.copy(isNotFound = true, isLoading = false) }
                        return@launch
                    }

                    val reputation = loadReputation(profileId)

                    _uiState.update {
                        it
                            .copy(
                                userProfile = userProfile,
                                displayName = userProfile.userName,
                                starRating = reputation?.fiveSystemScore ?: 0.0,
                                reputationScore = reputation?.totalScore ?: 0L,
                                isReputationUnknown = reputation == null,
                                isLoading = false,
                            ).let { updated -> updated.copy(canSendPrivateMessage = canSendPrivateMessage(updated)) }
                    }

                    observeReputation(profileId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Not `isNotFound`: the lookup crossing the network means this is just as likely a
                    // connection problem, and telling the user their peer does not exist would be wrong.
                    log.e(e) { "Failed to load peer profile" }
                    _uiState.update { it.copy(isLoadFailed = true, isLoading = false) }
                }
            }
    }

    /**
     * "Own" means any of my identities, not just the selected one — multiple profiles are supported
     * and this screen must never render for any of them.
     *
     * Checks the already-loaded owned-profiles flow first (no network round-trip, and its ids are
     * `UserProfileVO.id`, exactly what callers navigate with). Falls back to the identity-ids call
     * only when that flow hasn't been warmed yet, e.g. right after startup.
     */
    private suspend fun isOwnProfile(profileId: String): Boolean {
        val ownProfiles = userProfileServiceFacade.userProfiles.value
        if (ownProfiles.isNotEmpty()) {
            return ownProfiles.any { it.id == profileId }
        }
        return try {
            userProfileServiceFacade.getUserIdentityIds().contains(profileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Failed to read identity ids while checking own profile" }
            false
        }
    }

    /**
     * Returns null when the score is not known yet, as opposed to known-and-zero.
     *
     * The two failure shapes are not interchangeable. The client facade returns `Result.failure` for
     * an unknown peer in release builds, while in debug it calls the API and can throw instead.
     *
     * A *completed* failure is ambiguous but recoverable: `ClientReputationServiceFacade.getReputation`
     * reads a local snapshot filled asynchronously by `subscribeUserReputation()`, so "absent" means
     * either the peer has no reputation or nothing has loaded yet.
     * [ReputationServiceFacade.scoreByUserProfileId] separates the two — it starts empty and is only
     * ever replaced wholesale by a payload, so a non-empty map proves a snapshot arrived and this
     * peer is genuinely unscored.
     *
     * A *thrown* lookup carries no such information: the call never reached a verdict, so the
     * snapshot says nothing about this peer and the result stays unknown. Feeding it through the
     * fallback would render a transport error as a confident zero, which is exactly what this
     * function exists to prevent — the offerbook card the user tapped through may be showing 4.5
     * stars for the same peer.
     *
     * Both verdicts are re-taken whenever the snapshot changes — see [observeReputation].
     */
    private suspend fun loadReputation(profileId: String): ReputationScoreVO? {
        val result =
            try {
                reputationServiceFacade.getReputation(profileId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w(e) { "Failed to load reputation for peer" }
                return null
            }
        result.getOrNull()?.let { return it }
        return if (reputationServiceFacade.scoreByUserProfileId.value.isNotEmpty()) ZERO_REPUTATION else null
    }

    /**
     * Keeps the score current after the first paint. On the client flavour `getReputation` reads a
     * local cache filled asynchronously by the `REPUTATION` subscription, so opening this screen
     * before the first payload lands resolves to "unknown" — and without this, it would stay that way
     * for as long as the screen is open. The node fills its own map from the network just the same.
     *
     * The snapshot is the trigger, not the source: it carries scores, while the screen needs the whole
     * score object, so each pass re-asks [loadReputation] for this one peer. Narrowed to the two facts
     * that can change this peer's verdict — its own score, and whether anything has arrived at all,
     * which is what flips "unknown" to a real zero — because on the node that re-ask is not a lookup:
     * Bisq2 ranks a peer by sorting every score it holds, and every other peer's update would pay for
     * it.
     *
     * The first emission is deliberately not dropped: a `StateFlow` replays the current value, which
     * re-resolves to what [loadProfile] just wrote, and `_uiState` conflates the identical copy. That
     * closes the gap between that read and this subscription, where a dropped emission would
     * otherwise be lost for good.
     */
    private fun observeReputation(profileId: String) {
        reputationJob?.cancel()
        reputationJob =
            presenterScope.launch {
                reputationServiceFacade.scoreByUserProfileId
                    .map { scores -> scores[profileId] to scores.isNotEmpty() }
                    .distinctUntilChanged()
                    .collect {
                        val reputation = loadReputation(profileId)
                        _uiState.update {
                            it.copy(
                                starRating = reputation?.fiveSystemScore ?: 0.0,
                                reputationScore = reputation?.totalScore ?: 0L,
                                isReputationUnknown = reputation == null,
                            )
                        }
                    }
            }
    }

    /**
     * On Bisq Connect the capability set starts at the legacy baseline and only becomes accurate once
     * the node's manifest arrives. Reading it once meant a profile opened before then hid the button
     * for the life of the screen, with re-navigating the only way back.
     */
    private fun observePrivateChatSupport() {
        privateChatSupportJob?.cancel()
        privateChatSupportJob =
            presenterScope.launch {
                privateChatServiceFacade.isSupported.collect { isSupported ->
                    isPrivateChatSupported = isSupported
                    _uiState.update { it.copy(canSendPrivateMessage = canSendPrivateMessage(it)) }
                }
            }
    }

    /**
     * Binds to the facade's ignored-ids flow rather than tracking the state locally, so an
     * ignore/unignore performed elsewhere (chat context menu, ignored-users list) is reflected here
     * live. It is a StateFlow, so the current value arrives immediately and no seed call is needed.
     */
    private fun observeIgnoredState(profileId: String) {
        ignoredStateJob?.cancel()
        ignoredStateJob =
            presenterScope.launch {
                userProfileServiceFacade.ignoredProfileIds.collect { ignoredIds ->
                    val isIgnored = profileId in ignoredIds
                    _uiState.update {
                        // Recomputed here too, so ignoring hides the button immediately without
                        // waiting for a reload.
                        val updated = it.copy(isIgnored = isIgnored)
                        updated.copy(canSendPrivateMessage = canSendPrivateMessage(updated))
                    }
                }
            }
    }

    private fun onConfirmIgnore() {
        val profileId = this.profileId ?: return
        guardedSuspendAction(_isIgnoreActionEnabled, "onConfirmIgnore") {
            _uiState.update { it.copy(showIgnoreConfirmDialog = false) }
            try {
                // isIgnored is deliberately not set here — the ignoredProfileIds collector owns it,
                // so the two never disagree if the call fails.
                userProfileServiceFacade.ignoreUserProfile(profileId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to ignore peer" }
                handleError(e)
            }
        }
    }

    /**
     * No confirmation dialog: un-ignoring is fully reversible, so the design surfaces it as a plain
     * visible button. (The ignored-users list does confirm — there a mis-tap is harder to notice.)
     */
    private fun onUndoIgnore() {
        val profileId = this.profileId ?: return
        guardedSuspendAction(_isIgnoreActionEnabled, "onUndoIgnore") {
            try {
                userProfileServiceFacade.undoIgnoreUserProfile(profileId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to undo ignore for peer" }
                handleError(e)
            }
        }
    }

    /**
     * The single rule for offering a DM, so the two places that recompute it cannot drift apart.
     *
     * [PeerProfileUiState.isOwnProfile] is part of it even though `loadProfile` returns early for
     * own profiles: without it, this would depend on that early return to stay correct, and opening
     * a DM with yourself creates a `sorted(me, me)` channel that Bisq 2 then selects on the node.
     */
    private fun canSendPrivateMessage(state: PeerProfileUiState): Boolean =
        isPrivateChatSupported &&
            !state.isIgnored &&
            !state.isOwnProfile &&
            state.userProfile != null

    /**
     * Opens (or creates) the DM channel with this peer and navigates to it.
     *
     * Creating the channel is local-only in Bisq 2 — nothing reaches the peer until the first
     * message is sent — so this is safe to do on a tap.
     */
    private fun onSendPrivateMessage() {
        val profileId = this.profileId ?: return
        guardedSuspendAction(_isOpenPrivateChatEnabled, "onSendPrivateMessage") {
            _uiState.update { it.copy(isOpeningPrivateChat = true) }
            privateChatServiceFacade
                .findOrCreateChannel(profileId)
                .onSuccess { channelId -> navigateTo(NavRoute.PrivateChat(channelId)) }
                .onFailure { e ->
                    // ensureActive, not `if (e is CancellationException) throw e`: a cancellation only
                    // reaches this handler when it is NOT ours. WebSocketApiClient rethrows the caller's
                    // own and deliberately keeps a request timeout as a failure — and
                    // TimeoutCancellationException IS a CancellationException, so rethrowing on type
                    // would swallow the snackbar for a send that really did time out, and skip the
                    // isOpeningPrivateChat reset below, leaving the button spinning.
                    currentCoroutineContext().ensureActive()
                    log.e(e) { "Failed to open a private chat" }
                    // A withheld permission is not a connection problem, and telling the user to
                    // retry would send them in circles — only a re-pairing can fix it.
                    val message =
                        if (e is PrivateChatNotPermittedException) {
                            "mobile.privateChats.notPermitted".i18n()
                        } else {
                            "mobile.privateChats.openChat.failed".i18n()
                        }
                    showSnackbar(message, type = SnackbarType.ERROR)
                }
            _uiState.update { it.copy(isOpeningPrivateChat = false) }
        }
    }

    /**
     * Closes the dialog but holds on to [reportMessage]: reporting can fail on a dropped connection,
     * and losing the text the user just wrote would make them compose it a second time. The error
     * snackbar is `ReportUserPresenter`'s — raising a second one here would double it.
     */
    private fun onReportFailure(reportMessage: String) {
        _uiState.update { it.copy(showReportDialog = false, reportDraft = reportMessage) }
    }
}
