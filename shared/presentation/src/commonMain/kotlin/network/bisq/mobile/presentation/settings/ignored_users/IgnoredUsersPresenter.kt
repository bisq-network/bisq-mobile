package network.bisq.mobile.presentation.settings.ignored_users

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class IgnoredUsersPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter),
    IIgnoredUsersPresenter {
    private val _ignoredUsers = MutableStateFlow<List<UserProfileVO>>(emptyList())
    override val ignoredUsers: StateFlow<List<UserProfileVO>> get() = _ignoredUsers.asStateFlow()

    private val _ignoreUserId: MutableStateFlow<String> = MutableStateFlow("")
    override val ignoreUserId: StateFlow<String> get() = _ignoreUserId.asStateFlow()

    override val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    override fun onViewAttached() {
        super.onViewAttached()
        loadIgnoredUsers()
    }

    private fun loadIgnoredUsers() {
        presenterScope.launch {
            try {
                val ignoredUserIds = userProfileServiceFacade.getIgnoredUserProfileIds().toList()
                val userProfiles = userProfileServiceFacade.findUserProfiles(ignoredUserIds)
                _ignoredUsers.value = userProfiles
            } catch (e: Exception) {
                log.e(e) { "Failed to load ignored users" }
                _ignoredUsers.value = emptyList()
            }
        }
    }

    override fun unblockUser(userId: String) {
        _ignoreUserId.value = userId
    }

    override fun unblockUserConfirm(userId: String) {
        presenterScope.launch {
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
}
