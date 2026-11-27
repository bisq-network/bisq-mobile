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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.TradeStallClockRepository
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

    // Most tests observe a list the facade has fully synchronized; the eviction tests drive it.
    private val openTradesSynced = MutableStateFlow(true)

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
    fun `observeTrades emits PhaseEntered as a trade enters a phase`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val openTrades = MutableStateFlow(listOf(fakeTrade(tradeState = state, isSeller = false)))

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION

            verify(exactly = 1) { analytics.track(Trade.PhaseEntered(Trade.Phase.BUYER_2)) }
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

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BTC_CONFIRMED

            verify(exactly = 1) { analytics.track(Trade.Completed(Trade.Role.TAKER)) }
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

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            errorMessage.value = "boom"

            verify(exactly = 1) { analytics.track(Trade.Errored(Trade.Role.TAKER)) }
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

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            peersErrorMessage.value = "peer boom"

            verify(exactly = 1) { analytics.track(Trade.Errored(Trade.Role.TAKER)) }
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

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            errorMessage.value = "Bitcoin address length must not be longer than 62"

            verify(exactly = 1) { analytics.track(Trade.Errored(Trade.Role.TAKER)) }
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

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            peersErrorMessage.value = "Takers (buyers) Bitcoin amount is too high. market mismatch"

            verify(exactly = 1) { analytics.track(Trade.Errored(Trade.Role.TAKER)) }
            verify(exactly = 0) { analytics.captureException(any()) }
            scope.cancel()
        }

    @Test
    fun `observeTrades emits OutOfSyncFirstDetected once for a trade stuck in INIT past the threshold`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, clock = { TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS + 1 })
            val openTrades = MutableStateFlow(listOf(fakeTrade(takeOfferDate = 0L)))

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            // Several recheck ticks must still collapse into a single event for the same trade.
            advanceTimeBy(TradeAnalyticsTracker.OUT_OF_SYNC_RECHECK_MS * 3)

            verify(exactly = 1) { analytics.track(Trade.OutOfSyncFirstDetected) }
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

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            advanceTimeBy(TradeAnalyticsTracker.OUT_OF_SYNC_RECHECK_MS * 3)

            verify(exactly = 0) { analytics.track(Trade.OutOfSyncFirstDetected) }
            scope.cancel()
        }

    @Test
    fun `stall bucket is UNKNOWN with no witnessed transition`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val openTrades = MutableStateFlow(listOf(fakeTrade()))

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }

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

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
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

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            assertEquals(Trade.StallBucket.UNDER_1H, tracker.stallBucketFor("t1"))

            openTrades.value = emptyList()

            assertEquals(Trade.StallBucket.UNKNOWN, tracker.stallBucketFor("t1"))
            scope.cancel()
        }

    @Test
    fun `stall clock survives a restart when the state did not change`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            var nowMs = 0L
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            // First session witnesses a transition at t=0.
            val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val firstTracker = TradeAnalyticsTracker(analytics, clock = { nowMs }, stallClockRepository = repo)
            firstTracker.observeTrades(firstScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            firstScope.cancel()

            // "Restart": a fresh tracker on the same repository, trade still in the same state.
            nowMs = 2L * 60 * 60 * 1000
            val secondScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val secondTracker = TradeAnalyticsTracker(analytics, clock = { nowMs }, stallClockRepository = repo)
            secondTracker.observeTrades(secondScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }

            assertEquals(Trade.StallBucket.H1_TO_24H, secondTracker.stallBucketFor("t1"))
            secondScope.cancel()
        }

    @Test
    fun `state changed while the app was closed re-stamps the clock at reload`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            var nowMs = 0L
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val firstTracker = TradeAnalyticsTracker(analytics, clock = { nowMs }, stallClockRepository = repo)
            firstTracker.observeTrades(firstScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            firstScope.cancel()

            // The trade advanced "while the app was closed" — the change is witnessed at reload,
            // so the clock re-stamps to the reload time instead of keeping the stale timestamp.
            state.value = BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION
            nowMs = 2L * 60 * 60 * 1000
            val secondScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val secondTracker = TradeAnalyticsTracker(analytics, clock = { nowMs }, stallClockRepository = repo)
            secondTracker.observeTrades(secondScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }

            nowMs += 30L * 60 * 1000
            assertEquals(Trade.StallBucket.UNDER_1H, secondTracker.stallBucketFor("t1"))
            secondScope.cancel()
        }

    @Test
    fun `initial empty open list does not wipe persisted clocks`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            var nowMs = 0L
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val firstTracker = TradeAnalyticsTracker(analytics, clock = { nowMs }, stallClockRepository = repo)
            firstTracker.observeTrades(firstScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            firstScope.cancel()

            // At startup the open list is empty while trades load, and not synchronized yet — that
            // must not evict.
            nowMs = 2L * 60 * 60 * 1000
            openTradesSynced.value = false
            val openTrades = MutableStateFlow(emptyList<TradeItemPresentationModel>())
            val secondScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val secondTracker = TradeAnalyticsTracker(analytics, clock = { nowMs }, stallClockRepository = repo)
            secondTracker.observeTrades(secondScope, openTrades, openTradesSynced) { it.tradeId }
            openTrades.value = listOf(fakeTrade(tradeState = state))
            openTradesSynced.value = true

            assertEquals(Trade.StallBucket.H1_TO_24H, secondTracker.stallBucketFor("t1"))
            secondScope.cancel()
        }

    @Test
    fun `persisted clock is evicted when a trade leaves the open list`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, clock = { 0L }, stallClockRepository = repo)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val openTrades = MutableStateFlow(listOf(fakeTrade(tradeState = state)))

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            assertTrue(repo.fetch().map.containsKey("t1"))

            openTrades.value = emptyList()

            assertTrue(repo.fetch().map.isEmpty())
            scope.cancel()
        }

    @Test
    fun `persisted state name the enum no longer knows is skipped on seeding`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            repo.record("t1", "NO_SUCH_STATE", 0L)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, clock = { 5L * 60 * 1000 }, stallClockRepository = repo)

            tracker.observeTrades(scope, MutableStateFlow(listOf(fakeTrade())), openTradesSynced) { it.tradeId }

            assertEquals(Trade.StallBucket.UNKNOWN, tracker.stallBucketFor("t1"))
            scope.cancel()
        }

    @Test
    fun `datastore failures degrade to in-memory clocks without killing the observer`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            // Seed read, retainAll and record all throw — the collector must keep going and still
            // emit Completed, and the in-memory clock must still tick.
            val tracker = TradeAnalyticsTracker(analytics, clock = { 0L }, stallClockRepository = ThrowingTradeStallClockRepository())
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            tracker.observeTrades(scope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BTC_CONFIRMED

            verify(exactly = 1) { analytics.track(Trade.Completed(Trade.Role.TAKER)) }
            assertEquals(Trade.StallBucket.UNDER_1H, tracker.stallBucketFor("t1"))
            scope.cancel()
        }

    // Open trades replay their current state on every activation and on every launch, so each of
    // these events must be reported once per trade, not once per observation.

    @Test
    fun `Completed is not reported again after a restart while the finished trade stays open`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            TradeAnalyticsTracker(analytics, stallClockRepository = repo)
                .observeTrades(firstScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BTC_CONFIRMED
            firstScope.cancel()

            val secondScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            TradeAnalyticsTracker(analytics, stallClockRepository = repo)
                .observeTrades(secondScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }

            verify(exactly = 1) { analytics.track(Trade.Completed(Trade.Role.TAKER)) }
            secondScope.cancel()
        }

    @Test
    fun `Completed is not reported again when the trades service re-activates`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val tracker = TradeAnalyticsTracker(analytics)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.BTC_CONFIRMED)

            // A deactivate/reactivate cycle replaces the service scope and observes again.
            val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            tracker.observeTrades(firstScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }
            firstScope.cancel()
            val secondScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            tracker.observeTrades(secondScope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }

            verify(exactly = 1) { analytics.track(Trade.Completed(Trade.Role.TAKER)) }
            secondScope.cancel()
        }

    @Test
    fun `Errored is not reported again after a restart`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val errorMessage = MutableStateFlow<String?>("boom")

            repeat(2) {
                val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
                TradeAnalyticsTracker(analytics, stallClockRepository = repo)
                    .observeTrades(scope, MutableStateFlow(listOf(fakeTrade(errorMessage = errorMessage))), openTradesSynced) { it.tradeId }
                scope.cancel()
            }

            verify(exactly = 1) { analytics.track(Trade.Errored(Trade.Role.TAKER)) }
            verify(exactly = 1) { analytics.captureException(any<TradeProtocolException>()) }
        }

    @Test
    fun `PhaseEntered is not reported again after a restart`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val state = MutableStateFlow(BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION)

            repeat(2) {
                val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
                TradeAnalyticsTracker(analytics, stallClockRepository = repo)
                    .observeTrades(scope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }
                scope.cancel()
            }

            verify(exactly = 1) { analytics.track(Trade.PhaseEntered(Trade.Phase.BUYER_2)) }
        }

    @Test
    fun `OutOfSyncFirstDetected is not reported again after a restart`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()

            repeat(2) {
                val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
                TradeAnalyticsTracker(analytics, clock = { TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS + 1 }, stallClockRepository = repo)
                    .observeTrades(scope, MutableStateFlow(listOf(fakeTrade(takeOfferDate = 0L))), openTradesSynced) { it.tradeId }
                advanceTimeBy(TradeAnalyticsTracker.OUT_OF_SYNC_RECHECK_MS)
                scope.cancel()
            }

            verify(exactly = 1) { analytics.track(Trade.OutOfSyncFirstDetected) }
        }

    @Test
    fun `OutOfSyncRecovered is reported once when a trade reported out of sync leaves INIT`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, clock = { TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS + 1 })
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            tracker.observeTrades(scope, MutableStateFlow(listOf(fakeTrade(tradeState = state, takeOfferDate = 0L))), openTradesSynced) { it.tradeId }
            advanceTimeBy(TradeAnalyticsTracker.OUT_OF_SYNC_RECHECK_MS)
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            state.value = BisqEasyTradeStateEnum.BTC_CONFIRMED

            verify(exactly = 1) { analytics.track(Trade.OutOfSyncRecovered) }
            scope.cancel()
        }

    @Test
    fun `OutOfSyncRecovered is reported at reload when the trade left INIT while the app was closed`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val clock = { TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS + 1 }
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            TradeAnalyticsTracker(analytics, clock = clock, stallClockRepository = repo)
                .observeTrades(firstScope, MutableStateFlow(listOf(fakeTrade(tradeState = state, takeOfferDate = 0L))), openTradesSynced) { it.tradeId }
            advanceTimeBy(TradeAnalyticsTracker.OUT_OF_SYNC_RECHECK_MS)
            firstScope.cancel()

            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
            repeat(2) {
                val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
                TradeAnalyticsTracker(analytics, clock = clock, stallClockRepository = repo)
                    .observeTrades(scope, MutableStateFlow(listOf(fakeTrade(tradeState = state, takeOfferDate = 0L))), openTradesSynced) { it.tradeId }
                scope.cancel()
            }

            verify(exactly = 1) { analytics.track(Trade.OutOfSyncRecovered) }
        }

    @Test
    fun `OutOfSyncRecovered is never reported for a trade that was not out of sync`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics)
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

            tracker.observeTrades(scope, MutableStateFlow(listOf(fakeTrade(tradeState = state))), openTradesSynced) { it.tradeId }
            state.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION

            verify(exactly = 0) { analytics.track(Trade.OutOfSyncRecovered) }
            scope.cancel()
        }

    @Test
    fun `reported markers are evicted with the trade when it leaves the open list`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, stallClockRepository = repo)
            val openTrades = MutableStateFlow(listOf(fakeTrade(tradeState = MutableStateFlow(BisqEasyTradeStateEnum.BTC_CONFIRMED))))

            tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
            val entry = repo.fetch().map.getValue("t1")
            assertTrue(entry.reported.isNotEmpty())

            openTrades.value = emptyList()

            assertTrue(repo.fetch().map.isEmpty())
            scope.cancel()
        }

    // trade.taken only fires for the taker, so a maker-side trade needs its own start event, and
    // completion and errors carry the role so takers and makers convert separately.

    @Test
    fun `StartedAsMaker is reported once for a maker-side trade across restarts`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()

            repeat(2) {
                val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
                TradeAnalyticsTracker(analytics, stallClockRepository = repo)
                    .observeTrades(scope, MutableStateFlow(listOf(fakeTrade(isMaker = true))), openTradesSynced) { it.tradeId }
                scope.cancel()
            }

            verify(exactly = 1) { analytics.track(Trade.StartedAsMaker) }
        }

    @Test
    fun `StartedAsMaker is never reported for a taker-side trade`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

            TradeAnalyticsTracker(analytics).observeTrades(scope, MutableStateFlow(listOf(fakeTrade(isMaker = false))), openTradesSynced) { it.tradeId }

            verify(exactly = 0) { analytics.track(Trade.StartedAsMaker) }
            scope.cancel()
        }

    @Test
    fun `Completed and Errored carry the maker role for a maker-side trade`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val state = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            val errorMessage = MutableStateFlow<String?>(null)
            val trade = fakeTrade(isMaker = true, tradeState = state, errorMessage = errorMessage)

            TradeAnalyticsTracker(analytics).observeTrades(scope, MutableStateFlow(listOf(trade)), openTradesSynced) { it.tradeId }
            errorMessage.value = "boom"
            state.value = BisqEasyTradeStateEnum.BTC_CONFIRMED

            verify(exactly = 1) { analytics.track(Trade.Errored(Trade.Role.MAKER)) }
            verify(exactly = 1) { analytics.track(Trade.Completed(Trade.Role.MAKER)) }
            verify(exactly = 0) { analytics.track(Trade.Errored(Trade.Role.TAKER)) }
            verify(exactly = 0) { analytics.track(Trade.Completed(Trade.Role.TAKER)) }
            scope.cancel()
        }

    // Eviction may only trust a list the facade has fully synchronized: the node replays its open
    // trades one at a time, and both facades empty the list on teardown or re-pairing.

    @Test
    fun `a partial replay before the open trades are synchronized does not evict the others`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val confirmed = { MutableStateFlow(BisqEasyTradeStateEnum.BTC_CONFIRMED) }
            repeat(2) {
                openTradesSynced.value = false
                val openTrades = MutableStateFlow(emptyList<TradeItemPresentationModel>())
                val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
                TradeAnalyticsTracker(analytics, stallClockRepository = repo).observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }

                // Node-style replay: one trade at a time, synchronized only once all are in.
                openTrades.value = listOf(fakeTrade(id = "a", tradeState = confirmed()))
                openTrades.value = listOf(fakeTrade(id = "a", tradeState = confirmed()), fakeTrade(id = "b", tradeState = confirmed()))
                openTradesSynced.value = true
                scope.cancel()
            }

            verify(exactly = 2) { analytics.track(Trade.Completed(Trade.Role.TAKER)) }
            assertEquals(setOf("a", "b"), repo.fetch().map.keys)
        }

    @Test
    fun `clearing the open trades on teardown does not evict their markers`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val state = MutableStateFlow(BisqEasyTradeStateEnum.BTC_CONFIRMED)
            val openTrades = MutableStateFlow(listOf(fakeTrade(tradeState = state)))
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            TradeAnalyticsTracker(analytics, stallClockRepository = repo).observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }

            // deactivate() / clearState(): unsynchronized first, then emptied.
            openTradesSynced.value = false
            openTrades.value = emptyList()
            scope.cancel()

            assertTrue(repo.fetch().map.containsKey("t1"))
        }

    @Test
    fun `a trade gone from a synchronized list is evicted`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val repo = FakeTradeStallClockRepository()
            val openTrades = MutableStateFlow(listOf(fakeTrade(id = "a"), fakeTrade(id = "b")))
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            TradeAnalyticsTracker(analytics, stallClockRepository = repo).observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }

            openTrades.value = listOf(fakeTrade(id = "a"))

            assertEquals(setOf("a"), repo.fetch().map.keys)
            scope.cancel()
        }

    @Test
    fun `a trade leaving INIT while its out-of-sync report is pending still reports its recovery`() =
        runTest {
            val analytics = mockk<AnalyticsService>(relaxed = true)
            val stateB = MutableStateFlow(BisqEasyTradeStateEnum.INIT)
            // Both trades are picked by the same check; while "a" is being reported (a slow write),
            // "b" leaves INIT and the state collector handles it, before the check reaches "b".
            // A standard dispatcher, so the collectors really interleave during that write.
            val repo =
                FakeTradeStallClockRepository(
                    onMarkReported = { tradeId, marker ->
                        if (tradeId == "a" && marker == "out_of_sync") {
                            stateB.value = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
                            delay(1)
                        }
                    },
                )
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val tracker = TradeAnalyticsTracker(analytics, clock = { TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS + 1 }, stallClockRepository = repo)
            val openTrades =
                MutableStateFlow(
                    listOf(
                        fakeTrade(id = "a", takeOfferDate = 0L),
                        fakeTrade(id = "b", tradeState = stateB, takeOfferDate = 0L),
                    ),
                )

            // The ticker never ends, so the scope must be cancelled even when a verify fails, or
            // the test scheduler would run it forever.
            try {
                tracker.observeTrades(scope, openTrades, openTradesSynced) { it.tradeId }
                advanceTimeBy(2)

                verify(exactly = 2) { analytics.track(Trade.OutOfSyncFirstDetected) }
                verify(exactly = 1) { analytics.track(Trade.OutOfSyncRecovered) }
            } finally {
                scope.cancel()
            }
        }

    private fun fakeTrade(
        id: String = "t1",
        isSeller: Boolean = false,
        isMaker: Boolean = false,
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
        every { model.isMaker } returns isMaker
        every { model.errorMessage } returns errorMessage
        every { model.errorStackTrace } returns errorStackTrace
        every { model.peersErrorMessage } returns peersErrorMessage
        every { model.peersErrorStackTrace } returns peersErrorStackTrace
        val item = mockk<TradeItemPresentationModel>()
        every { item.bisqEasyTradeModel } returns model
        every { item.tradeId } returns id
        return item
    }

    private class FakeTradeStallClockRepository(
        private val onMarkReported: suspend (tradeId: String, marker: String) -> Unit = { _, _ -> },
    ) : TradeStallClockRepository {
        private val state = MutableStateFlow(TradeStallClockMap())

        override val data: Flow<TradeStallClockMap> get() = state

        override suspend fun record(
            tradeId: String,
            stateName: String,
            transitionAtMs: Long?,
        ) {
            state.update {
                val reported = it.map[tradeId]?.reported.orEmpty()
                it.copy(it.map + (tradeId to TradeStallClockEntry(stateName, transitionAtMs, reported)))
            }
        }

        override suspend fun markReported(
            tradeId: String,
            stateName: String,
            marker: String,
        ) {
            state.update {
                val entry = it.map[tradeId] ?: TradeStallClockEntry(stateName)
                it.copy(it.map + (tradeId to entry.copy(reported = entry.reported + marker)))
            }
            onMarkReported(tradeId, marker)
        }

        override suspend fun retainAll(tradeIds: Set<String>) {
            state.update { it.copy(it.map.filterKeys(tradeIds::contains)) }
        }
    }

    private class ThrowingTradeStallClockRepository : TradeStallClockRepository {
        override val data: Flow<TradeStallClockMap> get() = flow { throw RuntimeException("read boom") }

        override suspend fun record(
            tradeId: String,
            stateName: String,
            transitionAtMs: Long?,
        ): Unit = throw RuntimeException("write boom")

        override suspend fun markReported(
            tradeId: String,
            stateName: String,
            marker: String,
        ): Unit = throw RuntimeException("write boom")

        override suspend fun retainAll(tradeIds: Set<String>): Unit = throw RuntimeException("write boom")
    }
}
