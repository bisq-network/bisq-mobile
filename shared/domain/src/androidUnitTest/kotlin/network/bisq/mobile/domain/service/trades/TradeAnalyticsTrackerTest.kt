package network.bisq.mobile.domain.service.trades

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
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
}
