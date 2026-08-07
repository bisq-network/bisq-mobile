package network.bisq.mobile.presentation.peer_profile

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.main.MainPresenter

class PeerProfilePresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private companion object {
        /**
         * `ClientReputationServiceFacade.getReputation` returns a failure — not a zero score — for
         * any peer with no reputation yet, and only in release builds. Surfacing that as an error
         * would break the screen for exactly the peers a user most wants to inspect, so it is
         * mapped to this instead.
         */
        val ZERO_REPUTATION = ReputationScoreVO(totalScore = 0L, fiveSystemScore = 0.0, ranking = 0)
    }

    private val _uiState = MutableStateFlow(PeerProfileUiState())
    val uiState: StateFlow<PeerProfileUiState> = _uiState.asStateFlow()

    private val _isIgnoreActionEnabled = MutableStateFlow(true)
    val isIgnoreActionEnabled: StateFlow<Boolean> = _isIgnoreActionEnabled.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    private var initializedProfileId: String? = null
    private var ignoredStateJob: Job? = null

    /**
     * Idempotent: the screen's `LaunchedEffect` re-fires whenever this destination is revealed from
     * the back stack, and reloading then would flash the loading state over already-correct data.
     * Keyed on the id rather than a boolean so navigating to a different peer still reloads — which
     * is why the state is replaced wholesale and the previous ignored-state collector is cancelled
     * rather than left running alongside a second one.
     */
    fun initialize(profileId: String) {
        if (initializedProfileId == profileId) return
        initializedProfileId = profileId
        _uiState.value = PeerProfileUiState(profileId = profileId)
        loadProfile(profileId)
        observeIgnoredState(profileId)
    }

    fun onAction(action: PeerProfileUiAction) {
        when (action) {
            PeerProfileUiAction.OnRetryLoadClick -> onRetryLoad()

            PeerProfileUiAction.OnIgnoreClick ->
                _uiState.update { it.copy(showIgnoreConfirmDialog = true) }

            PeerProfileUiAction.OnConfirmIgnore -> onConfirmIgnore()

            PeerProfileUiAction.OnDismissIgnoreDialog ->
                _uiState.update { it.copy(showIgnoreConfirmDialog = false) }

            PeerProfileUiAction.OnUndoIgnoreClick -> onUndoIgnore()

            PeerProfileUiAction.OnReportClick ->
                _uiState.update { it.copy(showReportDialog = true) }

            PeerProfileUiAction.OnDismissReportDialog ->
                _uiState.update { it.copy(showReportDialog = false, reportDraft = null) }

            is PeerProfileUiAction.OnReportFailure -> onReportFailure(action.message, action.reportMessage)
        }
    }

    private fun onRetryLoad() {
        val profileId = _uiState.value.profileId
        if (profileId.isEmpty()) return
        _uiState.update { it.copy(isLoading = true, isLoadFailed = false) }
        loadProfile(profileId)
    }

    private fun loadProfile(profileId: String) {
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

                val reputation = loadReputationOrZero(profileId)

                _uiState.update {
                    it.copy(
                        userProfile = userProfile,
                        displayName = userProfile.userName,
                        starRating = reputation.fiveSystemScore,
                        reputationScore = reputation.totalScore,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                // Not `isNotFound`: the lookup crossing the network means this is just as likely a
                // connection problem, and telling the user their peer does not exist would be wrong.
                log.e(e) { "Failed to load peer profile $profileId" }
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
        return runCatching { userProfileServiceFacade.getUserIdentityIds() }
            .getOrDefault(emptyList())
            .contains(profileId)
    }

    /**
     * Unwraps twice on purpose: the client facade returns `Result.failure` for an unknown peer in
     * release builds, while in debug it calls the API and can throw instead.
     */
    private suspend fun loadReputationOrZero(profileId: String): ReputationScoreVO =
        runCatching { reputationServiceFacade.getReputation(profileId).getOrNull() }
            .getOrNull() ?: ZERO_REPUTATION

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
                    _uiState.update { it.copy(isIgnored = profileId in ignoredIds) }
                }
            }
    }

    private fun onConfirmIgnore() {
        val profileId = _uiState.value.profileId
        if (profileId.isEmpty()) return
        guardedSuspendAction(_isIgnoreActionEnabled, "onConfirmIgnore") {
            _uiState.update { it.copy(showIgnoreConfirmDialog = false) }
            try {
                // isIgnored is deliberately not set here — the ignoredProfileIds collector owns it,
                // so the two never disagree if the call fails.
                userProfileServiceFacade.ignoreUserProfile(profileId)
            } catch (e: Exception) {
                log.e(e) { "Failed to ignore $profileId" }
                handleError(e)
            }
        }
    }

    /**
     * No confirmation dialog: un-ignoring is fully reversible, so the design surfaces it as a plain
     * visible button. (The ignored-users list does confirm — there a mis-tap is harder to notice.)
     */
    private fun onUndoIgnore() {
        val profileId = _uiState.value.profileId
        if (profileId.isEmpty()) return
        guardedSuspendAction(_isIgnoreActionEnabled, "onUndoIgnore") {
            try {
                userProfileServiceFacade.undoIgnoreUserProfile(profileId)
            } catch (e: Exception) {
                log.e(e) { "Failed to undo ignore for $profileId" }
                handleError(e)
            }
        }
    }

    /**
     * Closes the dialog but holds on to [reportMessage]: reporting can fail on a dropped connection,
     * and losing the text the user just wrote would make them compose it a second time.
     */
    private fun onReportFailure(
        message: String,
        reportMessage: String,
    ) {
        _uiState.update { it.copy(showReportDialog = false, reportDraft = reportMessage) }
        showSnackbar(message, type = SnackbarType.ERROR)
    }
}
