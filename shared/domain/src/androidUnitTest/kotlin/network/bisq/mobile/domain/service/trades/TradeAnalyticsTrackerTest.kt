package network.bisq.mobile.domain.service.trades

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade
import network.bisq.mobile.domain.analytics.AnalyticsService
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TradeAnalyticsTrackerTest {
    private val stallTimeout = 45_000L

    @Test
    fun `action confirmed when the user's own state advances within the window`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, stallTimeout)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            val result = tracker.trackAction(Trade.Step.FIAT_SENT, state, scope) { Result.success(Unit) }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION

            assertTrue(result.isSuccess)
            verify { analytics.track(Trade.Action(Trade.Step.FIAT_SENT, Trade.Outcome.CONFIRMED)) }
        }

    @Test
    fun `action stalled when accepted but the state never advances`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, stallTimeout)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            tracker.trackAction(Trade.Step.FIAT_SENT, state, scope) { Result.success(Unit) }
            advanceTimeBy(stallTimeout + 1_000)

            verify { analytics.track(Trade.Action(Trade.Step.FIAT_SENT, Trade.Outcome.STALLED)) }
        }

    @Test
    fun `action failed captures the exception and never watches for a transition`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, stallTimeout)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            val result = tracker.trackAction(Trade.Step.FIAT_RECEIPT, state, scope) { Result.failure(RuntimeException("boom")) }

            assertTrue(result.isFailure)
            verify { analytics.track(Trade.Action(Trade.Step.FIAT_RECEIPT, Trade.Outcome.FAILED)) }
            verify { analytics.captureException(any()) }
            verify(exactly = 0) { analytics.track(Trade.Action(Trade.Step.FIAT_RECEIPT, Trade.Outcome.CONFIRMED)) }
            verify(exactly = 0) { analytics.track(Trade.Action(Trade.Step.FIAT_RECEIPT, Trade.Outcome.STALLED)) }
        }

    @Test
    fun `track forwards a lifecycle event straight to analytics`() {
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val tracker = TradeAnalyticsTracker(analytics)

        tracker.track(Trade.Taken)

        verify { analytics.track(Trade.Taken) }
    }

    @Test
    fun `observeTrades emits PhaseReached as a trade enters a phase`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val openTrades = MutableStateFlow(listOf(fakeTrade(tradeState = state, isSeller = false)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION

            verify { analytics.track(Trade.PhaseReached(Trade.Phase.BUYER_2)) }
            scope.cancel()
        }

    @Test
    fun `observeTrades emits Completed once when a trade reaches BTC_CONFIRMED`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val openTrades = MutableStateFlow(listOf(fakeTrade(tradeState = state)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BTC_CONFIRMED

            verify(exactly = 1) { analytics.track(Trade.Completed) }
            scope.cancel()
        }

    @Test
    fun `observeTrades emits Errored and captures the exception on a local error`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val errorMessage = MutableStateFlow<String?>(null)
            val openTrades = MutableStateFlow(listOf(fakeTrade(errorMessage = errorMessage)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            errorMessage.value = "boom"

            verify(exactly = 1) { analytics.track(Trade.Errored) }
            verify { analytics.captureException(any<TradeProtocolException>()) }
            scope.cancel()
        }

    @Test
    fun `observeTrades emits Errored on a peer-only error even when the local error stays null`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val peersErrorMessage = MutableStateFlow<String?>(null)
            val openTrades = MutableStateFlow(listOf(fakeTrade(peersErrorMessage = peersErrorMessage)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            peersErrorMessage.value = "peer boom"

            verify(exactly = 1) { analytics.track(Trade.Errored) }
            verify { analytics.captureException(any<TradeProtocolException>()) }
            scope.cancel()
        }

    private fun fakeTrade(
        id: String = "t1",
        isSeller: Boolean = false,
        tradeState: MutableStateFlow<BisqEasyTradeStateEnum> = MutableStateFlow(BisqEasyTradeStateEnum.INIT),
        errorMessage: MutableStateFlow<String?> = MutableStateFlow(null),
        errorStackTrace: MutableStateFlow<String?> = MutableStateFlow(null),
        peersErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null),
        peersErrorStackTrace: MutableStateFlow<String?> = MutableStateFlow(null),
    ): TradeItemPresentationModel {
        val model = mockk<BisqEasyTradeModel>()
        every { model.tradeState } returns tradeState
        every { model.isSeller } returns isSeller
        every { model.errorMessage } returns errorMessage
        every { model.errorStackTrace } returns errorStackTrace
        every { model.peersErrorMessage } returns peersErrorMessage
        every { model.peersErrorStackTrace } returns peersErrorStackTrace
        val item = mockk<TradeItemPresentationModel>()
        every { item.bisqEasyTradeModel } returns model
        every { item.tradeId } returns id
        return item
    }
}
