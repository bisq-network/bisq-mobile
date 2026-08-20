package network.bisq.mobile.presentation.community

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.presentation.tabs.tab.ITabContainerPresenter
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [CommunityTopBarAction]: renders the entry icon (with badge) only while a
 * segment is live, and routes taps to the presenter.
 */
class CommunityTopBarActionUiTest : BisqComposeUiTestBase() {
    private fun presenterWith(
        visible: Boolean,
        unread: Int = 0,
    ): ITabContainerPresenter =
        mockk<ITabContainerPresenter>(relaxed = true).also {
            every { it.communityIconVisible } returns MutableStateFlow(visible)
            every { it.communityUnreadCount } returns MutableStateFlow(unread)
            every { it.showAnimation } returns MutableStateFlow(false)
            justRun { it.openCommunityHub() }
        }

    @Test
    fun `renders nothing while no segment is live`() {
        val presenter = presenterWith(visible = false, unread = 3)

        setTestContent { CommunityTopBarAction(presenter) }

        composeTestRule.onNodeWithTag("community_topbar_icon").assertDoesNotExist()
    }

    @Test
    fun `renders the icon with its badge when live`() {
        val presenter = presenterWith(visible = true, unread = 3)

        setTestContent { CommunityTopBarAction(presenter) }

        composeTestRule.onNodeWithTag("community_topbar_icon").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()
    }

    @Test
    fun `tap routes to the presenter`() {
        val presenter = presenterWith(visible = true)

        setTestContent { CommunityTopBarAction(presenter) }
        composeTestRule.onNodeWithTag("community_topbar_icon").performClick()

        verify(exactly = 1) { presenter.openCommunityHub() }
    }
}
