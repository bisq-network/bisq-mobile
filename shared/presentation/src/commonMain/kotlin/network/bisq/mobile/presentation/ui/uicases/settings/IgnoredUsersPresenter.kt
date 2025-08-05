package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class IgnoredUsersPresenter(
    private val userProfileService: UserProfileServiceFacade, mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IIgnoredUsersPresenter {

    private val _ignoredUsers = MutableStateFlow<List<UserProfileVO>>(emptyList())
    override val ignoredUsers: StateFlow<List<UserProfileVO>> = _ignoredUsers

    private val _avatarMap: MutableStateFlow<Map<String, PlatformImage?>> = MutableStateFlow(emptyMap())
    override val avatarMap: StateFlow<Map<String, PlatformImage?>> = _avatarMap

    override fun onViewAttached() {
        super.onViewAttached()
        loadIgnoredUsers()
    }

    override fun onViewUnattaching() {
        _avatarMap.update { emptyMap() }
        super.onViewUnattaching()
    }

    private fun loadIgnoredUsers() {
        launchIO {
            try {
                val ignoredUserIds = userProfileService.getIgnoredUserProfileIds()

                val userProfiles = userProfileService.findUserProfiles(ignoredUserIds)

                userProfiles.forEach { it ->
                    if (_avatarMap.value[it.nym] == null) {
                        val currentAvatarMap = _avatarMap.value.toMutableMap()
                        currentAvatarMap[it.nym] = userProfileService.getUserAvatar(it)
                        _avatarMap.value = currentAvatarMap
                    }
                }

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