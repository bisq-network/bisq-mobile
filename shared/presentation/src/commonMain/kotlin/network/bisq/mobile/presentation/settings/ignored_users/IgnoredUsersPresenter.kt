package network.bisq.mobile.presentation.settings.ignored_users

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

class IgnoredUsersPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(IgnoredUsersUiState())
    val uiState: StateFlow<IgnoredUsersUiState> = _uiState.asStateFlow()

    // Owned by `guardedSuspendAction`, which needs its own MutableStateFlow handle; the UI reads it
    // through `uiState` only, so this flow stays the single source of truth for it.
    private val _isUnblockConfirmEnabled = MutableStateFlow(true)

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    // A retry supersedes the in-flight attempt instead of racing it: a slow first load can
    // otherwise land after a fast retry and overwrite the newer result.
    private var loadJob: Job? = null

    override fun onViewAttached() {
        super.onViewAttached()
        _isUnblockConfirmEnabled
            .onEach { enabled -> _uiState.update { it.copy(isUnblockConfirmEnabled = enabled) } }
            .launchIn(presenterScope)
        loadIgnoredUsers()
    }

    fun onAction(action: IgnoredUsersUiAction) {
        when (action) {
            IgnoredUsersUiAction.OnRetryLoadClick -> onRetryLoad()

            is IgnoredUsersUiAction.OnUnblockClick -> onUnblockClick(action.userId)

            IgnoredUsersUiAction.OnConfirmUnblock -> onConfirmUnblock()

            IgnoredUsersUiAction.OnDismissUnblockDialog -> onDismissUnblockDialog()

            is IgnoredUsersUiAction.OnPeerProfileClick -> onPeerProfileClick(action.userId)
        }
    }

    private fun onRetryLoad() {
        loadIgnoredUsers()
    }

    private fun onUnblockClick(userId: String) {
        _uiState.update { it.copy(unblockUserId = userId) }
    }

    private fun onDismissUnblockDialog() {
        _uiState.update { it.copy(unblockUserId = null) }
    }

    private fun onPeerProfileClick(userId: String) {
        navigateTo(NavRoute.PeerProfile(userId))
    }

    private fun loadIgnoredUsers() {
        loadJob?.cancel()
        loadJob =
            presenterScope.launch {
                _uiState.update { it.copy(isLoading = true, isLoadFailed = false) }
                try {
                    val ignoredUserIds = userProfileServiceFacade.getIgnoredUserProfileIds().toList()
                    val userProfiles = userProfileServiceFacade.findUserProfiles(ignoredUserIds)
                    _uiState.update { it.copy(ignoredUsers = userProfiles, isLoading = false) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Failed to load ignored users" }
                    _uiState.update { it.copy(ignoredUsers = emptyList(), isLoading = false, isLoadFailed = true) }
                }
            }
    }

    private fun onConfirmUnblock() {
        val userId = _uiState.value.unblockUserId ?: return
        guardedSuspendAction(_isUnblockConfirmEnabled, "unblockUserConfirm") {
            try {
                userProfileServiceFacade.undoIgnoreUserProfile(userId)
                _uiState.update { it.copy(unblockUserId = null) }
                loadIgnoredUsers()
            } catch (e: Exception) {
                log.e(e) { "Failed to unblock user: $userId" }
            }
        }
    }
}
