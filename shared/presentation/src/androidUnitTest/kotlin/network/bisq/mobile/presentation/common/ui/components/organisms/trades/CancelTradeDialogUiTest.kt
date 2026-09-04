package network.bisq.mobile.presentation.common.ui.components.organisms.trades

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade.InterruptReason
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [CancelTradeDialog]: warning copy per rejection/buyer/seller variant, the
 * analytics-gated reason chips (hidden unless opted in), and that the confirm callback
 * forwards the selected [InterruptReason] — falling back to UNSPECIFIED when none is picked.
 */
class CancelTradeDialogUiTest : BisqComposeUiTestBase() {
    private val reasonPrompt get() = "mobile.tradeInterrupt.reasonPrompt".i18n()
    private val priceMovedLabel get() = "mobile.tradeInterrupt.reason.priceMoved".i18n()

    @Test
    fun `when rejection then shows reject warning and hides reason prompt by default`() {
        val onConfirm = mockk<(InterruptReason) -> Unit>(relaxed = true)

        setTestContent {
            CancelTradeDialog(
                onCancelConfirm = onConfirm,
                onDismiss = {},
                isRejection = true,
            )
        }

        composeTestRule.onNodeWithText("bisqEasy.openTrades.rejectTrade.warning".i18n()).assertExists()
        composeTestRule.onNodeWithText(reasonPrompt).assertDoesNotExist()
    }

    @Test
    fun `when buyer cancels then shows buyer warning`() {
        setTestContent {
            CancelTradeDialog(
                onCancelConfirm = {},
                onDismiss = {},
                isRejection = false,
                isBuyer = true,
            )
        }

        val part2 = "bisqEasy.openTrades.cancelTrade.warning.part2".i18n()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.cancelTrade.warning.buyer".i18n(part2))
            .assertExists()
    }

    @Test
    fun `when seller cancels then shows seller warning`() {
        setTestContent {
            CancelTradeDialog(
                onCancelConfirm = {},
                onDismiss = {},
                isRejection = false,
                isBuyer = false,
            )
        }

        val part2 = "bisqEasy.openTrades.cancelTrade.warning.part2".i18n()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.cancelTrade.warning.seller".i18n(part2))
            .assertExists()
    }

    @Test
    fun `when reason chips shown then lists every reason except unspecified`() {
        setTestContent {
            CancelTradeDialog(
                onCancelConfirm = {},
                onDismiss = {},
                isRejection = false,
                showReasonChips = true,
            )
        }

        composeTestRule.onNodeWithText(reasonPrompt).assertExists()
        composeTestRule.onNodeWithText("mobile.tradeInterrupt.reason.peerUnresponsive".i18n()).assertExists()
        composeTestRule.onNodeWithText(priceMovedLabel).assertExists()
        composeTestRule.onNodeWithText("mobile.tradeInterrupt.reason.paymentMethodIssue".i18n()).assertExists()
        composeTestRule.onNodeWithText("mobile.tradeInterrupt.reason.noProgress".i18n()).assertExists()
        composeTestRule.onNodeWithText("mobile.tradeInterrupt.reason.tooComplex".i18n()).assertExists()
        composeTestRule.onNodeWithText("mobile.tradeInterrupt.reason.changedMind".i18n()).assertExists()
        composeTestRule.onNodeWithText("mobile.tradeInterrupt.reason.other".i18n()).assertExists()
        // Chips are the dialog's only selectable nodes: exactly seven guards against an
        // empty-labeled UNSPECIFIED chip sneaking past the per-label asserts above.
        composeTestRule.onAllNodes(isSelectable()).assertCountEquals(7)
    }

    @Test
    fun `when no reason selected and confirmed then forwards unspecified`() {
        val onConfirm = mockk<(InterruptReason) -> Unit>(relaxed = true)

        setTestContent {
            CancelTradeDialog(
                onCancelConfirm = onConfirm,
                onDismiss = {},
                isRejection = false,
                showReasonChips = true,
            )
        }

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()

        verify(exactly = 1) { onConfirm(InterruptReason.UNSPECIFIED) }
    }

    @Test
    fun `when reason selected and confirmed then forwards that reason`() {
        val onConfirm = mockk<(InterruptReason) -> Unit>(relaxed = true)

        setTestContent {
            CancelTradeDialog(
                onCancelConfirm = onConfirm,
                onDismiss = {},
                isRejection = false,
                showReasonChips = true,
            )
        }

        composeTestRule.onNodeWithText(priceMovedLabel).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()

        verify(exactly = 1) { onConfirm(InterruptReason.PRICE_MOVED) }
    }

    @Test
    fun `when selected reason tapped again then deselects and confirm falls back to unspecified`() {
        val onConfirm = mockk<(InterruptReason) -> Unit>(relaxed = true)

        setTestContent {
            CancelTradeDialog(
                onCancelConfirm = onConfirm,
                onDismiss = {},
                isRejection = false,
                showReasonChips = true,
            )
        }

        composeTestRule.onNodeWithText(priceMovedLabel).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(priceMovedLabel).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()

        verify(exactly = 1) { onConfirm(InterruptReason.UNSPECIFIED) }
    }

    @Test
    fun `when dismiss tapped then calls onDismiss and never confirms`() {
        val onConfirm = mockk<(InterruptReason) -> Unit>(relaxed = true)
        val onDismiss = mockk<() -> Unit>(relaxed = true)

        setTestContent {
            CancelTradeDialog(
                onCancelConfirm = onConfirm,
                onDismiss = onDismiss,
                isRejection = false,
            )
        }

        composeTestRule.onNodeWithContentDescription("dialog_confirm_no").performClick()

        verify(exactly = 1) { onDismiss() }
        verify(exactly = 0) { onConfirm(any()) }
    }
}
