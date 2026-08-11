package network.bisq.mobile.presentation.trade.trade_detail.states.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Before
import org.junit.Test

/**
 * UI tests for [State4Content] (Robolectric). Verifies rendering per [State4UiState] and action dispatch.
 */
class State4ContentUiTest : BisqComposeUiTestBase() {
    private lateinit var mockOnAction: (State4UiAction) -> Unit

    @Before
    fun setup() {
        mockOnAction = mockk(relaxed = true)
    }

    @Test
    fun when_trade_is_null_then_completed_header_not_displayed() {
        setTestContent {
            State4Content(
                uiState = State4UiState(trade = null),
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeCompleted.title".i18n())
            .assertDoesNotExist()
    }

    @Test
    fun when_trade_present_then_shows_title_labels_amounts_and_buttons() {
        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        // BtcSatsText parses a decimal BTC amount only (suffix is added by the component).
        every { trade.formattedBaseAmount } returns "0.01500000"
        every { trade.quoteAmountWithCode } returns "1,500.00 USD"

        val uiState =
            State4UiState(
                trade = trade,
                myDirectionLabel = "I bought",
                myOutcomeLabel = "I paid",
            )

        setTestContent {
            State4Content(uiState = uiState, onAction = mockOnAction)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("bisqEasy.tradeCompleted.title".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("I bought", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("BTC", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("1,500.00 USD").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.phase4.exportTrade".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.phase4.leaveChannel".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun when_export_clicked_then_dispatches_OnExportTradeClick() {
        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.formattedBaseAmount } returns "0.01"
        every { trade.quoteAmountWithCode } returns "100 USD"

        setTestContent {
            State4Content(
                uiState =
                    State4UiState(
                        trade = trade,
                        myDirectionLabel = "d",
                        myOutcomeLabel = "o",
                    ),
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.phase4.exportTrade".i18n())
            .performClick()

        verify(exactly = 1) { mockOnAction(State4UiAction.OnExportTradeClick) }
    }

    @Test
    fun when_close_trade_clicked_then_dispatches_OnCloseTradeClick() {
        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.formattedBaseAmount } returns "0.01"
        every { trade.quoteAmountWithCode } returns "100 USD"

        setTestContent {
            State4Content(
                uiState =
                    State4UiState(
                        trade = trade,
                        myDirectionLabel = "d",
                        myOutcomeLabel = "o",
                    ),
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.phase4.leaveChannel".i18n())
            .performClick()

        verify(exactly = 1) { mockOnAction(State4UiAction.OnCloseTradeClick) }
    }
}
