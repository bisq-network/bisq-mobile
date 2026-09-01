package network.bisq.mobile.domain.service.trades

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.TradeOutOfSyncDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun `timeout-style cancellation with an active caller is reported as FAILED`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, stallTimeout)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            val result =
                tracker.trackAction(Trade.Step.BTC_ADDRESS, state, scope) {
                    Result.failure(CancellationException("request timed out"))
                }

            assertTrue(result.isFailure)
            verify { analytics.track(Trade.Action(Trade.Step.BTC_ADDRESS, Trade.Outcome.FAILED)) }
            verify { analytics.captureException(any()) }
            scope.cancel()
        }

    @Test
    fun `genuine caller cancellation is not reported as FAILED`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, stallTimeout)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val gate = CompletableDeferred<Unit>()
            val started = CompletableDeferred<Unit>()

            var outcome: Result<Unit>? = null
            var propagated: CancellationException? = null
            val child =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        outcome =
                            tracker.trackAction(Trade.Step.BTC_RECEIVED, state, scope) {
                                started.complete(Unit)
                                try {
                                    gate.await()
                                    Result.success(Unit)
                                } catch (e: CancellationException) {
                                    // Simulate a wrap-into-Result.failure while OUR job is already
                                    // cancelled (HttpClientService / an inner catch). The tracker must
                                    // still refuse to report it.
                                    Result.failure(e)
                                }
                            }
                    } catch (e: CancellationException) {
                        propagated = e
                        throw e
                    }
                }

            started.await()
            child.cancel()
            child.join()

            assertNotNull(propagated)
            assertTrue(child.isCancelled)
            assertNull(outcome)
            verify(exactly = 0) { analytics.track(any()) }
            verify(exactly = 0) { analytics.captureException(any()) }
            scope.cancel()
        }

    @Test
    fun `non-CE failure is not reported when the caller is already cancelled`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, stallTimeout)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val gate = CompletableDeferred<Unit>()
            val started = CompletableDeferred<Unit>()

            var outcome: Result<Unit>? = null
            var propagated: CancellationException? = null
            val child =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        outcome =
                            tracker.trackAction(Trade.Step.BTC_ADDRESS, state, scope) {
                                started.complete(Unit)
                                try {
                                    gate.await()
                                    Result.success(Unit)
                                } catch (_: CancellationException) {
                                    Result.failure(RuntimeException("teardown boom"))
                                }
                            }
                    } catch (e: CancellationException) {
                        propagated = e
                        throw e
                    }
                }

            started.await()
            child.cancel()
            child.join()

            assertNotNull(propagated)
            assertTrue(child.isCancelled)
            assertNull(outcome)
            verify(exactly = 0) { analytics.track(any()) }
            verify(exactly = 0) { analytics.captureException(any()) }
            scope.cancel()
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
    fun `observeTrades emits Errored and captures unexpected local errors`() =
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
    fun `observeTrades emits Errored on a peer-only unexpected error even when the local error stays null`() =
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

    @Test
    fun `observeTrades does not capture expected protocol-validation rejections`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val errorMessage = MutableStateFlow<String?>(null)
            val openTrades = MutableStateFlow(listOf(fakeTrade(errorMessage = errorMessage)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            errorMessage.value = "Bitcoin address length must not be longer than 62"

            verify(exactly = 1) { analytics.track(Trade.Errored) }
            verify(exactly = 0) { analytics.captureException(any()) }
            scope.cancel()
        }

    @Test
    fun `observeTrades does not capture expected peer-reported amount rejections`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val peersErrorMessage = MutableStateFlow<String?>(null)
            val openTrades = MutableStateFlow(listOf(fakeTrade(peersErrorMessage = peersErrorMessage)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            peersErrorMessage.value = "Takers (buyers) Bitcoin amount is too high. market mismatch"

            verify(exactly = 1) { analytics.track(Trade.Errored) }
            verify(exactly = 0) { analytics.captureException(any()) }
            scope.cancel()
        }

    @Test
    fun `observeTrades emits OutOfSyncDetected once for a trade stuck in INIT past the threshold`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, clock = { TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS + 1 })
            val openTrades = MutableStateFlow(listOf(fakeTrade(takeOfferDate = 0L)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            // Several recheck ticks must still collapse into a single event for the same trade.
            advanceTimeBy(TradeAnalyticsTracker.OUT_OF_SYNC_RECHECK_MS * 3)

            verify(exactly = 1) { analytics.track(Trade.OutOfSyncDetected) }
            scope.cancel()
        }

    @Test
    fun `observeTrades stays silent for fresh INIT trades and trades that progressed`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val now = TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS * 2
            val tracker = TradeAnalyticsTracker(analytics, clock = { now })
            val openTrades =
                MutableStateFlow(
                    listOf(
                        // INIT but taken just now — within the threshold.
                        fakeTrade(id = "fresh", takeOfferDate = now),
                        // Old take date but the state advanced — not stuck.
                        fakeTrade(
                            id = "progressed",
                            tradeState = MutableStateFlow(BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION),
                            takeOfferDate = 0L,
                        ),
                    ),
                )

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            advanceTimeBy(TradeAnalyticsTracker.OUT_OF_SYNC_RECHECK_MS * 3)

            verify(exactly = 0) { analytics.track(Trade.OutOfSyncDetected) }
            scope.cancel()
        }

    @Test
    fun `stall bucket is UNKNOWN with no witnessed transition`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val openTrades = MutableStateFlow(listOf(fakeTrade()))

            tracker.observeTrades(scope, openTrades) { it.tradeId }

            // The initial state replay is a first sighting, not a transition — its age is unknowable.
            assertEquals(Trade.StallBucket.UNKNOWN, tracker.stallBucketFor("t1"))
            assertEquals(Trade.StallBucket.UNKNOWN, tracker.stallBucketFor("never-seen"))
            assertEquals(Trade.StallBucket.UNKNOWN, tracker.stallBucketFor(null))
            scope.cancel()
        }

    @Test
    fun `stall bucket measures time since the last witnessed transition`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            var nowMs = 0L
            val tracker = TradeAnalyticsTracker(analytics, clock = { nowMs })
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val openTrades = MutableStateFlow(listOf(fakeTrade(tradeState = state)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION // witnessed at nowMs = 0

            nowMs = 30L * 60 * 1000
            assertEquals(Trade.StallBucket.UNDER_1H, tracker.stallBucketFor("t1"))
            nowMs = 2L * 60 * 60 * 1000
            assertEquals(Trade.StallBucket.H1_TO_24H, tracker.stallBucketFor("t1"))
            nowMs = 2L * 24 * 60 * 60 * 1000
            assertEquals(Trade.StallBucket.D1_TO_3D, tracker.stallBucketFor("t1"))
            nowMs = 4L * 24 * 60 * 60 * 1000
            assertEquals(Trade.StallBucket.OVER_3D, tracker.stallBucketFor("t1"))
            scope.cancel()
        }

    @Test
    fun `stall entries are evicted when a trade leaves the open list`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, clock = { 0L })
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val openTrades = MutableStateFlow(listOf(fakeTrade(tradeState = state)))

            tracker.observeTrades(scope, openTrades) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            assertEquals(Trade.StallBucket.UNDER_1H, tracker.stallBucketFor("t1"))

            openTrades.value = emptyList()

            assertEquals(Trade.StallBucket.UNKNOWN, tracker.stallBucketFor("t1"))
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
        // "Just taken" so INIT trades in unrelated tests never trip the out-of-sync detector.
        takeOfferDate: Long = DateUtils.now(),
    ): TradeItemPresentationModel {
        val model = mockk<BisqEasyTradeModel>()
        every { model.tradeState } returns tradeState
        every { model.takeOfferDate } returns takeOfferDate
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
