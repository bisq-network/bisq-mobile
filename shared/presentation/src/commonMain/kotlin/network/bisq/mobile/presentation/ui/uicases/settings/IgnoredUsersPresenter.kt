package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    override val ignoredUsers: StateFlow<List<UserProfileVO>> get() = _ignoredUsers.asStateFlow()

    private val _avatarMap: MutableStateFlow<Map<String, PlatformImage?>> = MutableStateFlow(emptyMap())
    override val avatarMap: StateFlow<Map<String, PlatformImage?>> get() = _avatarMap.asStateFlow()

    private val _ignoreUserId: MutableStateFlow<String> = MutableStateFlow("")
    override val ignoreUserId: StateFlow<String> get() = _ignoreUserId.asStateFlow()

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
                val ignoredUserIds = userProfileService.getIgnoredUserProfileIds().toList()

                val userProfiles = userProfileService.findUserProfiles(ignoredUserIds)

                val newAvatars = mutableMapOf<String, PlatformImage?>()
                userProfiles.forEach { profile ->
                    if (_avatarMap.value[profile.nym] == null) {
                        newAvatars[profile.nym] = userProfileService.getUserAvatar(profile)
                    }
                }
                _avatarMap.update { currentMap ->
                    currentMap + newAvatars
                }

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
        launchIO {
            try {
                userProfileService.undoIgnoreUserProfile(userId)
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