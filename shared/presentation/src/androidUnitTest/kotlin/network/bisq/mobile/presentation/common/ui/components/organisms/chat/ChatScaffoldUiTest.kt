package network.bisq.mobile.presentation.common.ui.components.organisms.chat

import androidx.compose.material3.Text
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

/**
 * UI tests for [ChatScaffold] — the chat-screen layout contract (issue #1740): topBar and
 * content slots render alongside the [ChatInputBottomBar] bottom bar, and the chat-input
 * callbacks pass through the scaffold to the input.
 */
class ChatScaffoldUiTest : BisqComposeUiTestBase() {
    private val placeholder = "Type message"

    @Test
    fun `renders topBar, content and chat input together`() {
        setTestContent {
            ChatScaffold(
                onMessageSend = {},
                topBar = { Text("Chat title") },
                placeholder = placeholder,
            ) {
                Text("Message list goes here")
            }
        }

        composeTestRule.onNodeWithText("Chat title").assertExists()
        composeTestRule.onNodeWithText("Message list goes here").assertExists()
        composeTestRule.onNodeWithText(placeholder).assertExists()
    }

    @Test
    fun `send in the bottom bar routes text to onMessageSend`() {
        val onMessageSend = mockk<(String) -> Unit>(relaxed = true)

        setTestContent {
            ChatScaffold(
                onMessageSend = onMessageSend,
                placeholder = placeholder,
            ) {
                Text("content")
            }
        }

        composeTestRule.onNodeWithText(placeholder).performTextInput("hello")
        composeTestRule.onNodeWithContentDescription("Send icon").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onMessageSend("hello") }
    }

    @Test
    fun `quoted message renders in the bottom bar and close routes to onCloseReply`() {
        val onCloseReply = mockk<() -> Unit>(relaxed = true)
        val quoted =
            createMockBisqEasyOpenTradeMessage(
                id = "msg1",
                text = "Payment sent",
                senderUserProfile = createMockUserProfile("Alice"),
                myUserProfile = createMockUserProfile("Bob"),
            )

        setTestContent {
            ChatScaffold(
                onMessageSend = {},
                quotedMessage = quoted,
                placeholder = placeholder,
                onCloseReply = onCloseReply,
            ) {
                Text("content")
            }
        }

        composeTestRule.onNodeWithText("Payment sent").assertExists()
        composeTestRule.onNodeWithContentDescription("close").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onCloseReply() }
    }
}
