package network.bisq.mobile.presentation.common.ui.components.atoms.icons

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.CompletableDeferred
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [MyUserProfileIcon]: the placeholder stays on screen until the avatar image is
 * ready, so the top bar never shows an empty slot between the two.
 */
class MyUserProfileIconUiTest : BisqComposeUiTestBase() {
    private val image = PlatformImage(ImageBitmap(1, 1))

    @Test
    fun `placeholder shows while there is no profile`() {
        setIcon(userProfile = null, provider = { image })

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `placeholder stays until the avatar image is ready`() {
        val pendingImage = CompletableDeferred<PlatformImage>()
        setIcon(userProfile = createMockUserProfile("alice"), provider = { pendingImage.await() })

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertDoesNotExist()

        pendingImage.complete(image)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    @Test
    fun `avatar replaces the placeholder with animations disabled`() {
        setIcon(userProfile = createMockUserProfile("alice"), provider = { image }, showAnimations = false)

        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_AVATAR_TAG).assertExists()
        composeTestRule.onNodeWithTag(MY_USER_PROFILE_ICON_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    private fun setIcon(
        userProfile: UserProfileVO?,
        provider: suspend (UserProfileVO) -> PlatformImage,
        showAnimations: Boolean = true,
    ) {
        setTestContent {
            MyUserProfileIcon(
                userProfile = userProfile,
                userProfileIconProvider = provider,
                connectivityStatus = ConnectivityStatus.REQUESTING_INVENTORY,
                showAnimations = showAnimations,
            )
        }
    }
}
