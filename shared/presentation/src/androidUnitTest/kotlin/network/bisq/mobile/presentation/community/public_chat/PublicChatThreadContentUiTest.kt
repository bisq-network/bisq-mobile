package network.bisq.mobile.presentation.community.public_chat

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.font.FontWeight
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatMessage
import network.bisq.mobile.data.replicated.chat.common.createMockCommonPublicChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.CHAT_MENTION_PICKER_TAG
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals

/**
 * The thread body is a layout, not a scaffold: the hub mounts it inside its own `BisqScaffold` —
 * which already carries `imePadding()` — so a nested scaffold would apply the window insets twice
 * and reserve a bottom-bar height inside a weighted box. `SupportChannelScreen` supplies the chrome
 * from the other side with a `BisqScaffold` carrying only a top bar — not `ChatScaffold`, which owns a
 * `ChatInputBottomBar` and would give the screen a second composer.
 */
class PublicChatThreadContentUiTest : BisqComposeUiTestBase() {
    private val me: UserProfileVO = createMockUserProfile("me")
    private val alice: UserProfileVO = createMockUserProfile("alice")

    @Test
    fun `the loading state renders no messages`() {
        setTestContent { Content(PublicChatUiState(isLoading = true, messages = listOf(message("m1", "hello")))) }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("hello").assertDoesNotExist()
    }

    @Test
    fun `the message list renders once loaded`() {
        setTestContent {
            Content(
                PublicChatUiState(
                    isLoading = false,
                    channelId = "discussion.bisq",
                    messages = listOf(message("m1", "hello")),
                    readCount = 1,
                ),
            )
        }

        composeTestRule.onNodeWithText("hello").assertIsDisplayed()
    }

    @Test
    fun `a search reports how many messages matched`() {
        setTestContent {
            Content(
                PublicChatUiState(
                    isLoading = false,
                    channelId = "discussion.bisq",
                    messages = listOf(message("m1", "hello")),
                    readCount = 1,
                    searchQuery = "hell",
                    searchMatchCount = 1,
                ),
            )
        }

        composeTestRule.onNodeWithText("mobile.community.chat.search.matches".i18n(1)).assertIsDisplayed()
    }

    @Test
    fun `a search with no matches says so`() {
        setTestContent {
            Content(
                PublicChatUiState(
                    isLoading = false,
                    channelId = "discussion.bisq",
                    messages = emptyList(),
                    readCount = 0,
                    searchQuery = "nothing",
                    searchMatchCount = 0,
                ),
            )
        }

        composeTestRule.onNodeWithText("mobile.community.chat.search.noMatches".i18n()).assertIsDisplayed()
    }

    @Test
    fun `an empty channel says so rather than rendering nothing`() {
        setTestContent {
            Content(PublicChatUiState(isLoading = false, channelId = "discussion.bisq", messages = emptyList(), readCount = 0))
        }

        composeTestRule.onNodeWithText("mobile.community.chat.empty".i18n()).assertIsDisplayed()
    }

    /** A node without the public-chat capability is a terminal state, not a slow load. */
    @Test
    fun `an unsupported backend renders the unavailable state instead of the composer`() {
        setTestContent { Content(PublicChatUiState(isLoading = false, isSupported = false)) }

        composeTestRule.onNodeWithText("mobile.community.chat.notAvailable".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send icon").assertDoesNotExist()
    }

    /**
     * A delete is irreversible on the network, so it asks first — and the three dialogs are driven by
     * a target id in the state rather than by local composable state, so each one's wiring is worth
     * pinning where a rename would otherwise go unnoticed.
     */
    @Test
    fun `the delete dialog confirms through the presenter`() {
        var action: PublicChatUiAction? = null
        setTestContent {
            Content(
                uiState = PublicChatUiState(isLoading = false, channelId = "discussion.bisq", readCount = 0, deleteTargetMessageId = "m1"),
                onAction = { action = it },
            )
        }

        composeTestRule.onNodeWithText("bisqEasy.offerbook.chatMessage.deleteMessage.confirmation".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("action.delete".i18n()).performClick()

        assertEquals(PublicChatUiAction.OnConfirmDelete, action)
    }

    @Test
    fun `the ignore dialog confirms through the presenter`() {
        var action: PublicChatUiAction? = null
        setTestContent {
            Content(
                uiState = PublicChatUiState(isLoading = false, channelId = "discussion.bisq", readCount = 0, ignoreTargetProfileId = "p1"),
                onAction = { action = it },
            )
        }

        composeTestRule.onNodeWithText("chat.ignoreUser.confirm".i18n()).performClick()

        assertEquals(PublicChatUiAction.OnConfirmIgnore, action)
    }

    @Test
    fun `the undo ignore dialog confirms through the presenter`() {
        var action: PublicChatUiAction? = null
        setTestContent {
            Content(
                uiState =
                    PublicChatUiState(isLoading = false, channelId = "discussion.bisq", readCount = 0, undoIgnoreTargetProfileId = "p1"),
                onAction = { action = it },
            )
        }

        composeTestRule.onNodeWithText("user.profileCard.userActions.undoIgnore".i18n()).performClick()

        assertEquals(PublicChatUiAction.OnConfirmUndoIgnore, action)
    }

    @Test
    fun `mention candidates are forwarded to the composer picker`() {
        setTestContent {
            Content(
                PublicChatUiState(
                    isLoading = false,
                    channelId = "discussion.bisq",
                    messages = emptyList(),
                    readCount = 0,
                    mentionCandidates = listOf(createMockUserProfile("alice")),
                ),
            )
        }

        composeTestRule.onNodeWithText("chat.message.input.prompt".i18n()).performTextInput("@")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CHAT_MENTION_PICKER_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("alice").assertIsDisplayed()
    }

    @Test
    fun `owned mention ranges are forwarded to the message list`() {
        val body = "hey @me look"
        setTestContent {
            Content(
                PublicChatUiState(
                    isLoading = false,
                    channelId = "discussion.bisq",
                    messages = listOf(message("m1", body)),
                    readCount = 1,
                    myProfiles = listOf(me),
                ),
            )
        }

        val annotated =
            composeTestRule
                .onNodeWithText(body)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .first()
        val mentionStart = "hey ".length
        val mentionEnd = mentionStart + "@me".length
        val span = annotated.spanStyles.single { it.start == mentionStart && it.end == mentionEnd }

        assertEquals(FontWeight.Medium, span.item.fontWeight)
        assertEquals(BisqTheme.colors.primary, span.item.color)
    }

    @androidx.compose.runtime.Composable
    private fun Content(
        uiState: PublicChatUiState,
        onAction: (PublicChatUiAction) -> Unit = {},
    ) {
        PublicChatThreadContent(
            uiState = uiState,
            onAction = onAction,
            userProfileIconProvider = { createEmptyImage() },
            userNameProvider = { it },
            isSendChatMessageEnabled = true,
        )
    }

    private fun message(
        id: String,
        text: String,
    ): CommonPublicChatMessage =
        createMockCommonPublicChatMessage(
            id = id,
            text = text,
            date = 1234567890000L,
            senderUserProfile = alice,
            myUserProfile = me,
        )
}
