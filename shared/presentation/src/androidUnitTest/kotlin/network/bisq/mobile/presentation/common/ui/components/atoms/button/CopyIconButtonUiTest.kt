package network.bisq.mobile.presentation.common.ui.components.atoms.button

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * Covers the copy confirmation: the icon turns into a check mark on a successful copy and reverts
 * on its own, so the button carries its own feedback where a snackbar cannot be seen.
 */
class CopyIconButtonUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when copied then the check icon replaces the copy icon and reverts`() {
        setTestContent {
            CopyIconButton(value = "copy me", showToast = false)
        }

        composeTestRule.onNodeWithContentDescription("Copy icon").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("check").assertExists()

        composeTestRule.mainClock.advanceTimeBy(2_500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Copy icon").assertExists()
        composeTestRule.onNodeWithContentDescription("check").assertDoesNotExist()
    }

    @Test
    fun `an explicit icon size still renders both states`() {
        setTestContent {
            CopyIconButton(value = "copy me", showToast = false, iconSize = 18.dp)
        }

        composeTestRule.onNodeWithContentDescription("Copy icon").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("check").assertExists()
    }
}
