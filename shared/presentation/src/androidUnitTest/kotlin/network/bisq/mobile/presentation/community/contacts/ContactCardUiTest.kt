package network.bisq.mobile.presentation.community.contacts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/** UI tests for [ContactCard] (#1238): render contract of the directory row and its tap target. */
class ContactCardUiTest : BisqComposeUiTestBase() {
    private fun tagged() =
        sampleContact(
            id = "c1",
            peerName = "SatoshiFan#1234",
            trustScore = 0.92,
            contactReason = ContactReasonEnum.MANUALLY_ADDED,
            dateAddedLabel = "12 Jul 2026",
            tag = "Reliable SEPA trader",
        )

    @Test
    fun `tagged card renders name, date, tag pill, reason label and trust percentage`() {
        setTestContent {
            ContactCard(
                contact = tagged(),
                userProfileIconProvider = { createEmptyImage() },
                onClick = {},
            )
        }

        composeTestRule.onNodeWithText("SatoshiFan#1234").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 Jul 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reliable SEPA trader").assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.community.contacts.reason.manuallyAdded".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("92%").assertIsDisplayed()
    }

    @Test
    fun `untagged card renders without a tag pill and still shows the reason`() {
        setTestContent {
            ContactCard(
                contact =
                    sampleContact(
                        id = "c2",
                        peerName = "NewTrader#0007",
                        trustScore = 0.0,
                        contactReason = ContactReasonEnum.PRIVATE_CHAT,
                        dateAddedLabel = "2 days ago",
                    ),
                userProfileIconProvider = { createEmptyImage() },
                onClick = {},
            )
        }

        composeTestRule.onNodeWithText("NewTrader#0007").assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.community.contacts.reason.privateChat".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun `tapping anywhere on the card fires onClick`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setTestContent {
            ContactCard(
                contact = tagged(),
                userProfileIconProvider = { createEmptyImage() },
                onClick = onClick,
            )
        }

        composeTestRule.onNodeWithText("SatoshiFan#1234").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onClick() }
    }
}
