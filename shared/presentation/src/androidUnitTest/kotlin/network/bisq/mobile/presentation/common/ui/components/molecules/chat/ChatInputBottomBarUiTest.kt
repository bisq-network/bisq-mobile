package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.createMockBisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertFalse

/**
 * UI tests for [ChatInputBottomBar] — the shared bottom-bar placement for [ChatInputField]
 * (issue #1740). The component owns the standard scaffold-bottom-bar padding and passes the
 * input's callbacks/state through, so the tests pin the pass-through contract: send routes
 * the typed text, quoted-message close routes to onCloseReply, sendEnabled gates the send.
 */
class ChatInputBottomBarUiTest : BisqComposeUiTestBase() {
    private val placeholder = "Type message"

    @Test
    fun `typing and tapping send routes text to onMessageSend`() {
        val onMessageSend = mockk<(String) -> Unit>(relaxed = true)

        setTestContent {
            ChatInputBottomBar(
                onMessageSend = onMessageSend,
                placeholder = placeholder,
            )
        }

        composeTestRule.onNodeWithText(placeholder).performTextInput("hello")
        composeTestRule.onNodeWithContentDescription("Send icon").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onMessageSend("hello") }
    }

    @Test
    fun `quoted message is shown and close routes to onCloseReply`() {
        val onCloseReply = mockk<() -> Unit>(relaxed = true)
        val quoted =
            createMockBisqEasyOpenTradeMessage(
                id = "msg1",
                text = "Payment sent",
                senderUserProfile = createMockUserProfile("Alice"),
                myUserProfile = createMockUserProfile("Bob"),
            )

        setTestContent {
            ChatInputBottomBar(
                onMessageSend = {},
                quotedMessage = quoted,
                placeholder = placeholder,
                onCloseReply = onCloseReply,
            )
        }

        composeTestRule.onNodeWithText("Payment sent").assertExists()
        composeTestRule.onNodeWithContentDescription("close").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onCloseReply() }
    }

    @Test
    fun `sendEnabled false is passed through and blocks sending`() {
        var sent = false

        setTestContent {
            ChatInputBottomBar(
                onMessageSend = { sent = true },
                placeholder = placeholder,
                sendEnabled = false,
            )
        }

        composeTestRule.onNodeWithText(placeholder).performTextInput("hello")
        composeTestRule
            .onNodeWithContentDescription("Send icon")
            .assertIsNotEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(sent)
    }
}
