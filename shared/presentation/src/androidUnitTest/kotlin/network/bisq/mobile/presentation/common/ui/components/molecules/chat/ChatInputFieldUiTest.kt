package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import network.bisq.mobile.data.replicated.chat.ChatMessage
import network.bisq.mobile.data.replicated.chat.common.createMockCommonPublicChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Editing happens in the composer rather than in a dialog or inside the bubble: the composer already
 * owns the 10 000-character cap, its inline validation, the multiline growth and the blank-send
 * disabling, and putting a `TextField` inside the bubble would push one into a component trade chat
 * shares.
 */
class ChatInputFieldUiTest : BisqComposeUiTestBase() {
    private val me = createMockUserProfile("Bob")
    private val peer = createMockUserProfile("Alice")

    @Test
    fun `entering edit mode preloads the text and swaps send for save`() {
        setTestContent { InputField(editingMessageId = "msg-1", editingInitialText = "the original text") }

        composeTestRule.onNodeWithText("the original text").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Save icon").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send icon").assertDoesNotExist()
    }

    /**
     * Editing is a command to type, so the composer takes the focus — which opens the keyboard —
     * instead of waiting for a tap on a field that is already full of text.
     */
    @Test
    fun `entering an edit focuses the composer`() {
        setTestContent { InputField(editingMessageId = "msg-1", editingInitialText = "the original text") }

        composeTestRule.onNodeWithText("the original text").assertIsFocused()
    }

    /**
     * The cursor lands after the text, not in front of it: an edit is almost always a correction at
     * the end, and `BasicTextField`'s `String` overload starts its selection at zero.
     */
    @Test
    fun `entering an edit puts the cursor at the end of the text`() {
        setTestContent { InputField(editingMessageId = "msg-1", editingInitialText = "the original text") }

        composeTestRule
            .onNodeWithText("the original text")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange("the original text".length),
                ),
            )
    }

    @Test
    fun `the composer sends rather than saves when not editing`() {
        setTestContent { InputField() }

        composeTestRule.onNodeWithContentDescription("Send icon").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Save icon").assertDoesNotExist()
    }

    @Test
    fun `cancelling the edit reports it`() {
        var cancelled = false
        setTestContent {
            InputField(
                editingMessageId = "msg-1",
                editingInitialText = "the original text",
                onCancelEdit = { cancelled = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("close").performClick()

        assertTrue(cancelled)
    }

    /**
     * A reply and an edit are two different things to do with the composer, and bisq2 keeps the
     * original's citation on an edit — so the quote banner has nothing to offer while editing.
     */
    @Test
    fun `the edit banner replaces the quote banner rather than stacking with it`() {
        setTestContent {
            InputField(
                quotedMessage = quoted(),
                editingMessageId = "msg-1",
                editingInitialText = "the original text",
            )
        }

        composeTestRule.onNodeWithText("the quoted text").assertDoesNotExist()
        composeTestRule.onNodeWithText("the original text").assertIsDisplayed()
    }

    @Test
    fun `the quote banner still renders when not editing`() {
        setTestContent { InputField(quotedMessage = quoted()) }

        composeTestRule.onNodeWithText("the quoted text").assertIsDisplayed()
    }

    /**
     * A save the node refuses — a rate limit, a removal the local store rejects — leaves
     * `editingMessageId` set, because the presenter only clears the edit on success. If the composer
     * cleared itself on the way out the user would be left with the banner open, an empty field and
     * Save disabled, and the text they wrote gone.
     */
    @Test
    fun `a save that fails keeps the text to retry with`() {
        setTestContent { InputField(editingMessageId = "msg-1", editingInitialText = "the original text") }

        composeTestRule.onNodeWithText("the original text").performTextReplacement("the corrected text")
        composeTestRule.onNodeWithContentDescription("Save icon").performClick()

        composeTestRule.onNodeWithText("the corrected text").assertIsDisplayed()
    }

    /** The other half of the rule above: a send still clears, which is the documented trade-off. */
    @Test
    fun `a send clears the composer`() {
        setTestContent { InputField(placeholder = "type a message") }

        composeTestRule.onNodeWithText("type a message").performTextInput("a new message")
        composeTestRule.onNodeWithContentDescription("Send icon").performClick()

        composeTestRule.onNodeWithText("a new message").assertDoesNotExist()
    }

    @Test
    fun `an empty candidate list does not open the picker`() {
        setTestContent { InputField(placeholder = "type a message") }

        composeTestRule.onNodeWithText("type a message").performTextInput("@")

        composeTestRule.onNodeWithTag(CHAT_MENTION_PICKER_TAG).assertDoesNotExist()
    }

    @Test
    fun `typing an at token shows matching candidates and tap inserts the userName`() {
        val john = createMockUserProfile("john")
        val jane = createMockUserProfile("jane")
        setTestContent {
            InputField(placeholder = "type a message", mentionCandidates = listOf(john, jane))
        }

        composeTestRule.onNodeWithText("type a message").performTextInput("@jo")
        composeTestRule.onNodeWithTag(CHAT_MENTION_PICKER_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("john").assertIsDisplayed()
        composeTestRule.onNodeWithText("jane").assertDoesNotExist()

        composeTestRule.onNodeWithText("john").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("@john ").assertIsDisplayed()
        composeTestRule.onNodeWithTag(CHAT_MENTION_PICKER_TAG).assertDoesNotExist()
    }

    @Test
    fun `a query with no matches shows the empty placeholder`() {
        setTestContent {
            InputField(
                placeholder = "type a message",
                mentionCandidates = listOf(createMockUserProfile("john")),
            )
        }

        composeTestRule.onNodeWithText("type a message").performTextInput("@zzz")

        composeTestRule.onNodeWithText("chat.atMentionPopup.placeholder".i18n()).assertIsDisplayed()
    }

    /**
     * The picker is tap-only. Enter must still insert a newline while suggestions are open —
     * desktop's Enter-to-complete has no mobile analogue because Enter is a newline here.
     */
    @Test
    fun `enter inserts a newline while the picker is open`() {
        setTestContent {
            InputField(
                placeholder = "type a message",
                mentionCandidates = listOf(createMockUserProfile("john")),
            )
        }

        composeTestRule.onNodeWithText("type a message").performTextInput("@")
        composeTestRule.onNodeWithTag(CHAT_MENTION_PICKER_TAG).assertIsDisplayed()

        // Soft-keyboard Enter is a newline character here (maxLines is unbounded, no IME action).
        composeTestRule.onNodeWithText("@").performTextInput("\n")
        composeTestRule.waitForIdle()

        val editable =
            composeTestRule
                .onNodeWithText("@", substring = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
        assertTrue(editable.text.contains("\n"), "Enter must insert a newline rather than complete the mention")
    }

    @Test
    fun `the picker is suppressed while editing`() {
        setTestContent {
            InputField(
                editingMessageId = "msg-1",
                editingInitialText = "hey @",
                mentionCandidates = listOf(createMockUserProfile("Charlie")),
            )
        }

        composeTestRule.onNodeWithTag(CHAT_MENTION_PICKER_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Charlie").assertDoesNotExist()
    }

    @Composable
    private fun InputField(
        quotedMessage: ChatMessage<*>? = null,
        editingMessageId: String? = null,
        editingInitialText: String = "",
        placeholder: String = "",
        onCancelEdit: () -> Unit = {},
        mentionCandidates: List<UserProfileVO> = emptyList(),
    ) {
        ChatInputField(
            onMessageSend = {},
            quotedMessage = quotedMessage,
            placeholder = placeholder,
            editingMessageId = editingMessageId,
            editingInitialText = editingInitialText,
            onCancelEdit = onCancelEdit,
            mentionCandidates = mentionCandidates,
        )
    }

    private fun quoted() =
        createMockCommonPublicChatMessage(
            id = "msg-0",
            text = "the quoted text",
            senderUserProfile = peer,
            myUserProfile = me,
        )
}
