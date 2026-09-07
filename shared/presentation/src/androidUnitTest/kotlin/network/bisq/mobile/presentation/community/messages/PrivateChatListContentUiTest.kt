package network.bisq.mobile.presentation.community.messages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/** UI tests for [PrivateChatListContent] (#1825): loading vs empty vs populated, and click routing. */
class PrivateChatListContentUiTest : BisqComposeUiTestBase() {
    private fun conversation(
        channelId: String,
        name: String,
        unread: Long = 0,
    ) = sampleConversation(
        channelId = channelId,
        peerName = name,
        preview = "hello from $name",
        timeLabel = "2 min ago",
        epochMillis = 1_000L,
        starRating = 4.0,
        unreadCount = unread,
    )

    @Test
    fun `loading state renders the loading indicator, not the empty state`() {
        setTestContent {
            PrivateChatListContent(
                uiState = PrivateChatListUiState(isLoading = true),
                userProfileIconProvider = { createEmptyImage() },
                onAction = {},
            )
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun `empty state renders title, hint and the offerbook action once loaded`() {
        val onAction = mockk<(PrivateChatListUiAction) -> Unit>(relaxed = true)
        setTestContent {
            PrivateChatListContent(
                uiState = PrivateChatListUiState(isLoading = false),
                userProfileIconProvider = { createEmptyImage() },
                onAction = onAction,
            )
        }

        composeTestRule.onNodeWithText("mobile.privateChats.list.empty".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.privateChats.list.empty.hint".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.privateChats.list.empty.browseOfferbook".i18n()).performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onAction(PrivateChatListUiAction.OnBrowseOfferbookClick) }
    }

    @Test
    fun `populated list renders every conversation and click routes the tapped channel`() {
        val onAction = mockk<(PrivateChatListUiAction) -> Unit>(relaxed = true)
        setTestContent {
            PrivateChatListContent(
                uiState =
                    PrivateChatListUiState(
                        conversations =
                            listOf(
                                conversation("discussion.a-b", "Alice", unread = 3),
                                conversation("discussion.a-c", "Bob"),
                            ),
                    ),
                userProfileIconProvider = { createEmptyImage() },
                onAction = onAction,
            )
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("hello from Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onAction(PrivateChatListUiAction.OnConversationClick("discussion.a-c")) }
    }
}
