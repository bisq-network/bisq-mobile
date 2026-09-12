package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * The picker is given a firm height of at most four rows. Names past that stay in the
 * list and are reached by scrolling. The composer hosts it in a Popup so this height
 * never consumes thread layout.
 */
class ChatMentionPickerUiTest : BisqComposeUiTestBase() {
    @Test
    fun `a fifth candidate is reached by scrolling`() {
        setTestContent {
            ChatMentionPicker(
                profiles = (1..8).map { createMockUserProfile("User$it") },
                onSelect = {},
            )
        }

        composeTestRule.onNodeWithText("User1").assertIsDisplayed()
        composeTestRule.onNodeWithText("User4").assertIsDisplayed()
        composeTestRule.onNodeWithText("User8").assertDoesNotExist()

        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("User8"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("User8").assertIsDisplayed()
    }
}
