package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.ui.test.onNodeWithText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import kotlin.test.Test

/**
 * A trade in raw INIT used to render a fully blank step 1 — the "dead box" a user with a stuck
 * trade stares at. This pins the waiting fallback for that branch.
 */
class TradeFlowPaneUiTest : PresentationKoinComposeTestBase() {
    @Test
    fun `raw INIT phase renders the waiting fallback instead of a blank step`() {
        val presenter = mockk<TradeFlowPresenter>(relaxed = true)
        every { presenter.tradePhaseState } returns MutableStateFlow(TradeFlowPresenter.TradePhaseState.INIT)
        every { presenter.steps } returns
            listOf(
                TradeFlowPresenter.TradeFlowStep.ACCOUNT_DETAILS,
                TradeFlowPresenter.TradeFlowStep.FIAT_PAYMENT,
                TradeFlowPresenter.TradeFlowStep.BITCOIN_TRANSFER,
                TradeFlowPresenter.TradeFlowStep.TRADE_COMPLETED,
            )
        every { presenter.presenterForPhase(any()) } returns null

        setTestContent {
            TradeFlowPane(presenter)
        }

        composeTestRule.onNodeWithText("mobile.bisqEasy.openTrades.waitingForTradeData".i18n()).assertExists()
    }
}
