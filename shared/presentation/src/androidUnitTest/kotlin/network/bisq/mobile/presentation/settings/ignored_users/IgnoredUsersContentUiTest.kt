package network.bisq.mobile.presentation.settings.ignored_users

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals

class IgnoredUsersContentUiTest : BisqComposeUiTestBase() {
    private val peer = createMockUserProfile("SatoshiFan")

    private val iconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

    private fun setTestContent(
        uiState: IgnoredUsersUiState,
        onAction: (IgnoredUsersUiAction) -> Unit = {},
    ) {
        setTestContent {
            IgnoredUsersContent(
                uiState = uiState,
                userProfileIconProvider = iconProvider,
                onAction = onAction,
            )
        }
    }

    @Test
    fun `while loading then shows the loading indicator`() {
        setTestContent(uiState = IgnoredUsersUiState(isLoading = true))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun `when the load failed then shows the error and retry dispatches OnRetryLoadClick`() {
        var capturedAction: IgnoredUsersUiAction? = null
        setTestContent(
            uiState = IgnoredUsersUiState(isLoading = false, isLoadFailed = true),
            onAction = { capturedAction = it },
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.settings.ignoredUsers.loadFailed".i18n())
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("mobile.action.retry".i18n()).performClick()
        composeTestRule.waitForIdle()

        assertEquals(IgnoredUsersUiAction.OnRetryLoadClick, capturedAction)
    }

    @Test
    fun `when no ignored users then shows the empty message`() {
        setTestContent(uiState = IgnoredUsersUiState(isLoading = false))
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.settings.ignoredUsers.empty".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when unblock clicked then dispatches OnUnblockClick for that peer`() {
        var capturedAction: IgnoredUsersUiAction? = null
        setTestContent(
            uiState = IgnoredUsersUiState(ignoredUsers = listOf(peer), isLoading = false),
            onAction = { capturedAction = it },
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.settings.ignoredUsers.unblock".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(IgnoredUsersUiAction.OnUnblockClick(peer.networkId.pubKey.id), capturedAction)
    }

    @Test
    fun `when the confirmation is open then confirming dispatches OnConfirmUnblock`() {
        var capturedAction: IgnoredUsersUiAction? = null
        setTestContent(
            uiState =
                IgnoredUsersUiState(
                    ignoredUsers = listOf(peer),
                    isLoading = false,
                    unblockUserId = peer.networkId.pubKey.id,
                ),
            onAction = { capturedAction = it },
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        assertEquals(IgnoredUsersUiAction.OnConfirmUnblock, capturedAction)
    }
}
