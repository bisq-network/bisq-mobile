package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [UnreadCountBadge]: renders the formatted pill for positive counts,
 * caps above 99, and emits nothing at zero.
 */
class UnreadCountBadgeUiTest : BisqComposeUiTestBase() {
    @Test
    fun `positive count renders as its exact number`() {
        setTestContent {
            UnreadCountBadge(count = 5)
        }

        composeTestRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun `count above 99 renders capped`() {
        setTestContent {
            UnreadCountBadge(count = 120)
        }

        composeTestRule.onNodeWithText("99+").assertExists()
    }

    @Test
    fun `zero count renders no badge`() {
        setTestContent {
            UnreadCountBadge(count = 0)
        }

        composeTestRule.onNodeWithText("0").assertDoesNotExist()
    }
}
