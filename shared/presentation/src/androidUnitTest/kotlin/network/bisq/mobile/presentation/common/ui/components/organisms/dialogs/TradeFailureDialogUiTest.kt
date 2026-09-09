package network.bisq.mobile.presentation.common.ui.components.organisms.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

class TradeFailureDialogUiTest : BisqComposeUiTestBase() {
    @Test
    fun `shows headline header full error footer and close dismisses`() {
        val onClose = mockk<() -> Unit>(relaxed = true)
        val error =
            "Takers (buyers) Bitcoin amount is too high. " +
                "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                "to manipulate the price."

        setTestContent {
            TradeFailureDialog(
                errorMessage = error,
                onClose = onClose,
            )
        }

        composeTestRule.onNodeWithText("bisqEasy.openTrades.failure.popup.headline".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.failure.popup.message.header".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText(error).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.takeOffer.failure.footer".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("action.dontShowAgain".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText("mobile.community.support.openChannel".i18n()).assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        verify(exactly = 1) { onClose() }
    }

    @Test
    fun `peer rejection uses the at-peer headline`() {
        setTestContent {
            TradeFailureDialog(
                errorMessage = "Could not find matching offer",
                onClose = {},
                atPeer = true,
            )
        }

        composeTestRule.onNodeWithText("bisqEasy.openTrades.atPeer.failure.popup.headline".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.failure.popup.headline".i18n()).assertDoesNotExist()
    }

    @Test
    fun `shows the support channel and opens it`() {
        val onClose = mockk<() -> Unit>(relaxed = true)
        val onOpenSupportChannel = mockk<() -> Unit>(relaxed = true)

        setTestContent {
            TradeFailureDialog(
                errorMessage = "Could not find matching offer",
                onClose = onClose,
                showSupportChannel = true,
                onOpenSupportChannel = onOpenSupportChannel,
            )
        }

        composeTestRule.onNodeWithText("mobile.community.support.openChannel".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.community.support.openChannel".i18n()).performClick()
        verify(exactly = 1) { onOpenSupportChannel() }
        verify(exactly = 0) { onClose() }
    }

    @Test
    fun `hides the support channel when it is unavailable`() {
        setTestContent {
            TradeFailureDialog(
                errorMessage = "Could not find matching offer",
                onClose = {},
                showSupportChannel = false,
            )
        }

        composeTestRule.onNodeWithText("bisqEasy.openTrades.failure.popup.headline".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.community.support.openChannel".i18n()).assertDoesNotExist()
    }
}
