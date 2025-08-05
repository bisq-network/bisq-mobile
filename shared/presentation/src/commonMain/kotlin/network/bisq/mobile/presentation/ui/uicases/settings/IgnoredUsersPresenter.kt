package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class IgnoredUsersPresenter(
    private val userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IIgnoredUsersPresenter {

    private val _ignoredUsers = MutableStateFlow<List<UserProfileVO>>(emptyList())
    override val ignoredUsers: StateFlow<List<UserProfileVO>> = _ignoredUsers

    override fun onViewAttached() {
        super.onViewAttached()
        loadIgnoredUsers()
    }

    private fun loadIgnoredUsers() {
        launchIO {
            try {
                val ignoredUserIds = userProfileService.getIgnoredUserProfileIds()

                log.d { ignoredUserIds.toString() }

                val userProfiles = userProfileService.findUserProfiles(ignoredUserIds)
                log.d { userProfiles.toString() }

                _ignoredUsers.value = userProfiles
            } catch (e: Exception) {
                log.e(e) { "Failed to load ignored users" }
                _ignoredUsers.value = emptyList()
            }
        }
    }

    override fun unblockUser(userId: String) {
        launchIO {
            try {
                userProfileService.undoIgnoreUserProfile(userId)
                loadIgnoredUsers()
            } catch (e: Exception) {
                log.e(e) { "Failed to unblock user: $userId" }
            }
        }
    }
}