package network.bisq.mobile.presentation.common.ui.components.organisms.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import network.bisq.mobile.data.replicated.chat.ChatMessage
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.createMockBisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.common.createMockCommonPublicChatMessage
import network.bisq.mobile.data.replicated.chat.reactions.ChatMessageReaction
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.trade.TradePeerLeftMessageBox
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Renders the list through its required parameters only, which is how `TradeChatScreen` and the
 * private chat screen both call it — everything else is a defaulted no-op lambda.
 *
 * The inner `LazyColumn` fills its parent, so the list needs a bounded height or it collapses and
 * nothing renders. `reverseLayout = true`, so index 0 is the newest message at the bottom.
 */
class ChatMessageListUiTest : BisqComposeUiTestBase() {
    private val me = createMockUserProfile("Bob")
    private val peer = createMockUserProfile("Alice")

    @Test
    fun `renders the conversation with defaulted callbacks`() {
        setTestContent {
            MessageList(
                listOf(
                    message("msg-2", sender = me, text = "Just sent it over."),
                    message("msg-1", sender = peer, text = "Payment received, thanks!"),
                ),
            )
        }

        composeTestRule.onNodeWithText("Just sent it over.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Payment received, thanks!").assertIsDisplayed()
    }

    @Test
    fun `renders an empty conversation without crashing`() {
        setTestContent { MessageList(emptyList<BisqEasyOpenTradeMessage>()) }

        composeTestRule.waitForIdle()
    }

    /**
     * A `LEAVE` message goes through the `leaveMessageContent` slot instead of the text bubble. The
     * slot has no default: a caller inside a trade passes the trade wording, one outside it its own.
     */
    @Test
    fun `a leave message renders the default trade wording, not a text bubble`() {
        setTestContent {
            MessageList(
                listOf(
                    message("msg-2", sender = peer, text = null, type = ChatMessageTypeEnum.LEAVE),
                    message("msg-1", sender = peer, text = "Payment received, thanks!"),
                ),
                leaveMessageContent = { message, modifier -> TradePeerLeftMessageBox(message, modifier) },
            )
        }

        composeTestRule.onNodeWithText("bisqEasy.openTrades.chat.peerLeft.subHeadline".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Payment received, thanks!").assertIsDisplayed()
    }

    /**
     * A public channel message has no delivery status and no peer, and the list must not need
     * either: it is generic over the shared [ChatMessage] base, not the private branch.
     */
    @Test
    fun `renders a public channel conversation`() {
        setTestContent {
            MessageList(
                listOf(
                    createMockCommonPublicChatMessage(id = "msg-2", text = "Welcome to Bisq", senderUserProfile = me, myUserProfile = me),
                    createMockCommonPublicChatMessage(id = "msg-1", text = "Hi all", senderUserProfile = peer, myUserProfile = me),
                ),
            )
        }

        composeTestRule.onNodeWithText("Welcome to Bisq").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi all").assertIsDisplayed()
    }

    /**
     * The button carries a down arrow and the unread badge, so it has one destination: the newest
     * message, index 0 under `reverseLayout`. Opening with nothing read anchors the list at the
     * oldest message, which is where the jump used to strand the reader instead.
     */
    @Test
    fun `the jump to bottom button scrolls to the newest message`() {
        setTestContent { MessageList(conversation(), readCount = 0) }

        // The button shows up 400ms after the list reports it can scroll down.
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithTag(JUMP_TO_BOTTOM_TAG).assertIsDisplayed()

        composeTestRule.onNodeWithTag(JUMP_TO_BOTTOM_TAG).performClick()

        composeTestRule.onNodeWithText("Message 30").assertIsDisplayed()
        // Reaching the bottom reports everything read, which takes the button with it.
        composeTestRule.onNodeWithTag(JUMP_TO_BOTTOM_TAG).assertDoesNotExist()
    }

    /**
     * Pins a placement that reads as off by one in the source. The divider is emitted *before* the
     * message box inside the item at `unreadMarkerIndex` — the newest read message — but
     * `reverseLayout` reverses the placement of an item's own children too, so it lands below that
     * message and above the oldest unread one, which is the boundary it means. Moving it a row to
     * "fix" the reading breaks it.
     */
    @Test
    fun `the unread divider sits above the oldest unread message`() {
        // 5 unread, so the list opens anchored at "Message 26" with "Message 25" right above it.
        setTestContent { MessageList(conversation(), readCount = 25) }

        val divider = composeTestRule.onNodeWithText("mobile.chat.unreadMessages".i18n()).getUnclippedBoundsInRoot()
        val newestRead = composeTestRule.onNodeWithText("Message 25").getUnclippedBoundsInRoot()
        val oldestUnread = composeTestRule.onNodeWithText("Message 26").getUnclippedBoundsInRoot()

        assertTrue(divider.top >= newestRead.bottom, "the divider must sit below the newest read message")
        assertTrue(divider.bottom <= oldestUnread.top, "the divider must sit above the oldest unread message")
    }

    @Test
    fun `owned mention ranges are highlighted in the message body`() {
        val body = "hey @Bob look"
        setTestContent {
            MessageList(
                listOf(message("msg-1", sender = peer, text = body)),
                myProfiles = listOf(me),
            )
        }

        val annotated =
            composeTestRule
                .onNodeWithText(body)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .first()
        val mentionStart = "hey ".length
        val mentionEnd = mentionStart + "@Bob".length
        val span = annotated.spanStyles.single { it.start == mentionStart && it.end == mentionEnd }

        assertEquals(FontWeight.Medium, span.item.fontWeight)
        assertEquals(BisqTheme.colors.primary, span.item.color)
    }

    @Test
    fun `a citation of an owned profile does not highlight the body`() {
        setTestContent {
            MessageList(
                listOf(
                    createMockCommonPublicChatMessage(
                        id = "msg-1",
                        text = "disagree",
                        citation =
                            Citation(
                                authorUserProfileId = me.id,
                                text = "the original",
                                chatMessageId = "msg-0",
                            ),
                        citationAuthorUserProfile = me,
                        senderUserProfile = peer,
                        myUserProfile = me,
                    ),
                ),
                myProfiles = listOf(me),
            )
        }

        val annotated =
            composeTestRule
                .onNodeWithText("disagree")
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .first()
        assertTrue(annotated.spanStyles.isEmpty())
    }

    /**
     * [readCount] defaults to everything read, which is what the rendering tests want: it keeps the
     * unread divider and the jump-to-bottom button out of the way. The scroll tests pass their own.
     */
    @Composable
    private fun <M : ChatMessage<R>, R : ChatMessageReaction> MessageList(
        messages: List<M>,
        readCount: Int = messages.size,
        myProfiles: Collection<UserProfileVO> = emptyList(),
        leaveMessageContent: @Composable (M, Modifier) -> Unit = { _, _ -> },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatMessageList(
                messages = messages,
                ignoredUserIds = emptySet(),
                showChatRulesWarnBox = false,
                readCount = readCount,
                userProfileIconProvider = { { createEmptyImage() } },
                onResendMessage = {},
                userNameProvider = { it },
                onPeerProfileClick = {},
                modifier = Modifier.fillMaxSize(),
                myProfiles = myProfiles,
                leaveMessageContent = leaveMessageContent,
            )
        }
    }

    /**
     * 30 messages, newest first: "Message 30" is at index 0 and renders at the bottom. Long enough
     * that the list scrolls, which both the jump button and the unread divider need.
     */
    private fun conversation() =
        List(30) { i ->
            message("msg-${30 - i}", sender = if (i % 2 == 0) peer else me, text = "Message ${30 - i}")
        }

    private fun message(
        id: String,
        sender: UserProfileVO,
        text: String?,
        type: ChatMessageTypeEnum = ChatMessageTypeEnum.TEXT,
    ) = createMockBisqEasyOpenTradeMessage(
        id = id,
        chatMessageType = type,
        text = text,
        senderUserProfile = sender,
        myUserProfile = me,
    )

    private companion object {
        const val JUMP_TO_BOTTOM_TAG = "jump_to_bottom_button"
    }
}
