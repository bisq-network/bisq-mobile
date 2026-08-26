package network.bisq.mobile.presentation.peer_profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for the contact-details surface on Peer Profile (#1238): the add/remove button,
 * the private "My contact details" section, and the atomic edit dialog — all driven through
 * [PeerProfileScreenContent] with crafted states.
 */
class PeerProfileContactDetailsUiTest : BisqComposeUiTestBase() {
    private lateinit var mockOnAction: (PeerProfileUiAction) -> Unit

    override fun setUpUiTest() {
        super.setUpUiTest()
        mockOnAction = mockk(relaxed = true)
    }

    private fun loadedState(
        isContact: Boolean = false,
        showContactAction: Boolean = true,
        contactDetails: ContactDetailsUiState? = null,
        showEditDialog: Boolean = false,
        contactDraft: ContactDetailsUiState? = null,
    ) = PeerProfileUiState(
        userProfile = createMockUserProfile("Alice"),
        displayName = "Alice",
        isLoading = false,
        isContact = isContact,
        showContactAction = showContactAction,
        contactDetails = contactDetails,
        showEditContactDetailsDialog = showEditDialog,
        contactDraft = contactDraft,
    )

    private fun render(uiState: PeerProfileUiState) {
        setTestContent {
            PeerProfileScreenContent(
                uiState = uiState,
                userProfileIconProvider = { createEmptyImage() },
                onAction = mockOnAction,
            )
        }
    }

    @Test
    fun `non-contact shows Add to contacts and tapping fires the add action`() {
        render(loadedState(isContact = false))

        composeTestRule
            .onNodeWithText("mobile.peerProfile.contacts.add".i18n())
            .assertExists()
            .performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { mockOnAction(PeerProfileUiAction.OnAddContactClick) }
    }

    @Test
    fun `contact shows Remove from contacts and the details section, tapping edit opens the dialog`() {
        render(
            loadedState(
                isContact = true,
                contactDetails = ContactDetailsUiState(tag = "great seller", notes = "met at conf", trustScore = 0.94),
            ),
        )

        // Existence (not displayed/click): the profile body is not a scroll container, so
        // below-the-fold nodes can't be scrolled into the test viewport. The edit-click
        // dispatch is covered by the presenter tests.
        composeTestRule.onNodeWithText("mobile.peerProfile.contacts.remove".i18n()).assertExists()
        composeTestRule.onNodeWithText("mobile.peerProfile.contactDetails.title".i18n()).assertExists()
        composeTestRule.onNodeWithText("great seller").assertExists()
        composeTestRule.onNodeWithText("94%").assertExists()
        composeTestRule.onNodeWithText("met at conf").assertExists()
        composeTestRule.onNodeWithText("action.edit".i18n()).assertExists()
    }

    @Test
    fun `empty annotations render the no-tag and no-notes placeholders`() {
        render(loadedState(isContact = true, contactDetails = ContactDetailsUiState()))

        composeTestRule.onNodeWithText("mobile.peerProfile.contactDetails.noTag".i18n()).assertExists()
        composeTestRule.onNodeWithText("mobile.peerProfile.contactDetails.noNotes".i18n()).assertExists()
    }

    @Test
    fun `contact action is absent when the feature is not live`() {
        render(loadedState(isContact = false, showContactAction = false))

        composeTestRule.onNodeWithText("mobile.peerProfile.contacts.add".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText("mobile.peerProfile.contactDetails.title".i18n()).assertDoesNotExist()
    }

    @Test
    fun `edit dialog renders the draft and save-cancel dispatch their actions`() {
        render(
            loadedState(
                isContact = true,
                contactDetails = ContactDetailsUiState(tag = "old"),
                showEditDialog = true,
                contactDraft = ContactDetailsUiState(tag = "old", notes = "note", trustScore = 0.5),
            ),
        )

        composeTestRule.onNodeWithText("mobile.peerProfile.contactDetails.editTitle".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()

        composeTestRule.onNodeWithText("action.save".i18n()).performClick()
        composeTestRule.waitForIdle()
        verify(exactly = 1) { mockOnAction(PeerProfileUiAction.OnSaveContactDetailsClick) }

        composeTestRule.onNodeWithText("action.cancel".i18n()).performClick()
        composeTestRule.waitForIdle()
        verify(exactly = 1) { mockOnAction(PeerProfileUiAction.OnDismissEditContactDetailsDialog) }
    }
}
