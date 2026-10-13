package network.bisq.mobile.data.service.user_profile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.utils.PlatformImage

/**
 * Keeps [icon] in line with [selectedUserProfile] until cancelled.
 *
 * The image is cleared as soon as another profile (or none) is selected, so the image of the
 * previous profile is never shown for the new one. An update of the same profile keeps its image
 * while it is loaded again, as the image only depends on the identity of the profile. A newer
 * selection cancels a load which is still running, so a slow load cannot overwrite a later one.
 */
suspend fun keepSelectedUserProfileIconUpdated(
    selectedUserProfile: StateFlow<UserProfileVO?>,
    icon: MutableStateFlow<PlatformImage?>,
    loadIcon: suspend (UserProfileVO) -> PlatformImage,
) {
    var iconProfileId: String? = null
    selectedUserProfile.collectLatest { userProfile ->
        if (userProfile?.id != iconProfileId) {
            iconProfileId = null
            icon.value = null
        }
        if (userProfile != null) {
            try {
                icon.value = loadIcon(userProfile)
                iconProfileId = userProfile.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                iconProfileId = null
                icon.value = null
            }
        }
    }
}
