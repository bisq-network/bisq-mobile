package network.bisq.mobile.presentation.community.contacts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/** UI tests for [ContactsListContent] (#1238): loading vs empty vs populated, and click routing. */
class ContactsListContentUiTest : BisqComposeUiTestBase() {
    private fun entry(
        id: String,
        name: String,
    ) = sampleContact(
        id = id,
        peerName = name,
        trustScore = 0.5,
        contactReason = ContactReasonEnum.MANUALLY_ADDED,
        dateAddedLabel = "today",
    )

    @Test
    fun `loading state renders the loading indicator, not the empty state`() {
        setTestContent {
            ContactsListContent(
                uiState = ContactsListUiState(isLoading = true),
                userProfileIconProvider = { createEmptyImage() },
                onContactClick = {},
            )
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun `empty state renders title and hint once loaded`() {
        setTestContent {
            ContactsListContent(
                uiState = ContactsListUiState(isLoading = false),
                userProfileIconProvider = { createEmptyImage() },
                onContactClick = {},
            )
        }

        composeTestRule.onNodeWithText("mobile.community.contacts.empty.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.community.contacts.empty.hint".i18n()).assertIsDisplayed()
    }

    @Test
    fun `populated list renders every contact and click routes the tapped id`() {
        val onContactClick = mockk<(String) -> Unit>(relaxed = true)
        setTestContent {
            ContactsListContent(
                uiState = ContactsListUiState(contacts = listOf(entry("id-a", "Alice"), entry("id-b", "Bob"))),
                userProfileIconProvider = { createEmptyImage() },
                onContactClick = onContactClick,
            )
        }

        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onContactClick("id-b") }
    }
}
