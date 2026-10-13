package network.bisq.mobile.presentation.common.ui.components.atoms.icons

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.onNodeWithTag
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [MyUserProfileIcon]: the placeholder shows while there is no avatar image, so the
 * top bar never shows an empty slot, and an image which is already loaded shows at once.
 */
class MyUserProfileIconUiTest : BisqComposeUiTestBase() {
    private val image = PlatformImage(ImageBitmap(1, 1))

    @Test
    fun `placeholder shows while there is no profile`() {
        setIcon(userProfile = null, avatar = null)

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `placeholder stays until the avatar image is ready`() {
        var avatar by mutableStateOf<PlatformImage?>(null)
        setTestContent {
            MyUserProfileIcon(
                userProfile = createMockUserProfile("alice"),
                avatar = avatar,
                connectivityStatus = ConnectivityStatus.REQUESTING_INVENTORY,
                showAnimations = true,
            )
        }

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertDoesNotExist()

        avatar = image
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    @Test
    fun `avatar which is already loaded shows at once`() {
        setIcon(userProfile = createMockUserProfile("alice"), avatar = image)

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    @Test
    fun `avatar shows with animations disabled`() {
        setIcon(userProfile = createMockUserProfile("alice"), avatar = image, showAnimations = false)

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    private fun setIcon(
        userProfile: UserProfileVO?,
        avatar: PlatformImage?,
        showAnimations: Boolean = true,
    ) {
        setTestContent {
            MyUserProfileIcon(
                userProfile = userProfile,
                avatar = avatar,
                connectivityStatus = ConnectivityStatus.REQUESTING_INVENTORY,
                showAnimations = showAnimations,
            )
        }
    }
}
