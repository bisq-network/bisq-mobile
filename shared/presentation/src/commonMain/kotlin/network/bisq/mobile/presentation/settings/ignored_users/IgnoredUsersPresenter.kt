package network.bisq.mobile.presentation.settings.ignored_users

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : BasePresenter(mainPresenter),
    IIgnoredUsersPresenter {
    private val _ignoredUsers = MutableStateFlow<List<UserProfileVO>>(emptyList())
    override val ignoredUsers: StateFlow<List<UserProfileVO>> = _ignoredUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadFailed = MutableStateFlow(false)
    override val isLoadFailed: StateFlow<Boolean> = _isLoadFailed.asStateFlow()

    private val _ignoreUserId: MutableStateFlow<String> = MutableStateFlow("")
    override val ignoreUserId: StateFlow<String> = _ignoreUserId.asStateFlow()

    private val _isUnblockUserConfirmEnabled = MutableStateFlow(true)
    override val isUnblockUserConfirmEnabled: StateFlow<Boolean> = _isUnblockUserConfirmEnabled.asStateFlow()

    // A retry supersedes the in-flight attempt instead of racing it: on the client flavour a slow
    // first load can otherwise land after a fast retry and overwrite the newer result.
    private var loadJob: Job? = null

    override val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    override fun onViewAttached() {
        super.onViewAttached()
        loadIgnoredUsers()
    }

    private fun loadIgnoredUsers() {
        loadJob?.cancel()
        loadJob =
            presenterScope.launch {
                _isLoading.value = true
                _isLoadFailed.value = false
                try {
                    val ignoredUserIds = userProfileServiceFacade.getIgnoredUserProfileIds().toList()
                    val userProfiles = userProfileServiceFacade.findUserProfiles(ignoredUserIds)
                    _ignoredUsers.value = userProfiles
                    _isLoading.value = false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // On the client flavour this is a node round-trip, so a failure here is a
                    // connectivity problem rather than "no ignored peers" — say so and offer a retry.
                    log.e(e) { "Failed to load ignored users" }
                    _ignoredUsers.value = emptyList()
                    _isLoadFailed.value = true
                    _isLoading.value = false
                }
            }
    }

    override fun onRetryLoad() {
        loadIgnoredUsers()
    }

    override fun unblockUser(userId: String) {
        _ignoreUserId.value = userId
    }

    override fun unblockUserConfirm(userId: String) {
        guardedSuspendAction(_isUnblockUserConfirmEnabled, "unblockUserConfirm") {
            try {
                userProfileServiceFacade.undoIgnoreUserProfile(userId)
                _ignoreUserId.value = ""
                loadIgnoredUsers()
            } catch (e: Exception) {
                log.e(e) { "Failed to unblock user: $userId" }
            }
        }
    }

    override fun dismissConfirm() {
        _ignoreUserId.value = ""
    }

    override fun openPeerProfile(userId: String) {
        navigateTo(NavRoute.PeerProfile(userId))
    }
}
