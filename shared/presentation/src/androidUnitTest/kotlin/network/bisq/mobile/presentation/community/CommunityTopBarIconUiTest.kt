package network.bisq.mobile.presentation.community

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertTrue

/**
 * UI tests for [CommunityTopBarIcon]: badge shown for positive counts (capped at 99+),
 * hidden at zero, icon clickable in both states.
 */
class CommunityTopBarIconUiTest : BisqComposeUiTestBase() {
    @Test
    fun `positive unread count renders the badge`() {
        setTestContent {
            CommunityTopBarIcon(unreadCount = 5, showAnimation = false, onClick = {})
        }

        composeTestRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun `count above 99 renders capped`() {
        setTestContent {
            CommunityTopBarIcon(unreadCount = 120, showAnimation = false, onClick = {})
        }

        composeTestRule.onNodeWithText("99+").assertIsDisplayed()
    }

    @Test
    fun `zero unread renders the icon without a badge and stays clickable`() {
        var clicked = false
        setTestContent {
            CommunityTopBarIcon(unreadCount = 0, showAnimation = false, onClick = { clicked = true })
        }

        composeTestRule.onNodeWithText("0").assertDoesNotExist()
        composeTestRule.onNodeWithTag("community_topbar_icon").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `tap with unread present fires onClick`() {
        var clicked = false
        setTestContent {
            CommunityTopBarIcon(unreadCount = 3, showAnimation = false, onClick = { clicked = true })
        }

        composeTestRule.onNodeWithTag("community_topbar_icon").performClick()
        assertTrue(clicked)
    }
}
