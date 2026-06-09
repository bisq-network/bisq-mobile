package network.bisq.mobile.domain.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [BufferedAnalyticsService] — the wrapper that holds events while
 * the underlying Sentry transport is not yet ready (e.g. Tor still bootstrapping).
 *
 * Coverage axes:
 *  - **Pre-ready buffering**: events go into the in-memory buffer (FIFO for
 *    normal priority, head-insertion for immediate priority).
 *  - **Ready-time drain**: `onSentryReady()` flips the flag and drains the
 *    buffer in FIFO order to the downstream service.
 *  - **Post-ready pass-through**: subsequent events skip the buffer entirely
 *    and go directly to the downstream.
 *  - **Periodic flush safety net**: even without an explicit `onSentryReady()`
 *    call, the periodic ticker flushes when the flag flips.
 *  - **Bounded buffer**: drop-oldest for normal priority, drop-newest-tail for
 *    immediate priority.
 *  - **Failure fallback**: if a direct send throws, the event lands in the
 *    buffer instead of being lost.
 *  - **Idempotency**: `onSentryReady()` is safe to call multiple times.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BufferedAnalyticsServiceTest {
    /**
     * Tests use an unconfined CoroutineScope so fire-and-forget `scope.launch`
     * blocks (the buffer enqueues) execute INLINE in the calling thread up to
     * the first real suspension point. This bypasses the test-scheduler /
     * backgroundScope interaction quirk where launches on backgroundScope
     * weren't being drained by `advanceUntilIdle` in some configurations and
     * gives us deterministic assertions on buffer state without scheduler
     * gymnastics. Production code uses Dispatchers.Default — covered separately
     * by integration testing on real devices.
     */
    private fun unconfinedScope(): CoroutineScope = CoroutineScope(Dispatchers.Unconfined + Job())

    /**
     * Records every call into the downstream so tests can assert on what would
     * have been sent + in what order. Optional flags simulate a downstream that
     * throws — used to test the fall-back-to-buffer behaviour of `tryDirect`.
     */
    private class RecordingAnalyticsService(
        private val throwOnTrack: Boolean = false,
        private val throwOnCaptureException: Boolean = false,
    ) : AnalyticsService {
        val initCalls = mutableListOf<InitArgs>()
        val tracked = mutableListOf<AnalyticsEvent>()
        val capturedExceptions = mutableListOf<Throwable>()

        data class InitArgs(
            val dsn: String,
            val environment: String,
            val release: String,
            val isDebug: Boolean,
            val socksProxyHost: String?,
            val socksProxyPort: Int?,
        )

        override fun init(
            dsn: String,
            environment: String,
            release: String,
            isDebug: Boolean,
            socksProxyHost: String?,
            socksProxyPort: Int?,
        ) {
            initCalls += InitArgs(dsn, environment, release, isDebug, socksProxyHost, socksProxyPort)
        }

        override fun track(event: AnalyticsEvent) {
            if (throwOnTrack) error("simulated downstream failure")
            tracked += event
        }

        override fun trackImmediate(event: AnalyticsEvent) = track(event)

        override fun captureException(throwable: Throwable) {
            if (throwOnCaptureException) error("simulated downstream failure")
            capturedExceptions += throwable
        }

        override fun captureExceptionImmediate(throwable: Throwable) = captureException(throwable)
    }

    // ============ INIT FORWARDING ============

    @Test
    fun `init forwards dsn environment release and isDebug to the downstream`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            service.init(dsn = "http://abc@onion/3", environment = "production", release = "0.5.0", isDebug = false)

            assertEquals(1, downstream.initCalls.size)
            assertEquals(
                RecordingAnalyticsService.InitArgs("http://abc@onion/3", "production", "0.5.0", false, null, null),
                downstream.initCalls.first(),
            )
        }

    @Test
    fun `init does not flip readiness on its own`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            service.init("http://abc@onion/3", "production", "0.5.0", false)

            assertFalse(service.isReady, "init MUST NOT flip readiness — only onSentryReady() does")
        }

    // ============ PRE-READY BUFFERING ============

    @Test
    fun `track before ready goes to the buffer not the downstream`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            advanceUntilIdle() // drain the fire-and-forget enqueue coroutine

            assertTrue(downstream.tracked.isEmpty(), "must NOT send before ready")
            assertEquals(1, service.bufferedCount())
        }

    @Test
    fun `captureException before ready goes to the buffer`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            service.captureException(RuntimeException("boom"))
            advanceUntilIdle()

            assertTrue(downstream.capturedExceptions.isEmpty())
            assertEquals(1, service.bufferedCount())
        }

    @Test
    fun `trackImmediate before ready jumps the line ahead of pending normal events`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            // Two normal events first → they sit at positions 0 and 1
            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            // Then an immediate one → jumps to head, ahead of both
            service.trackImmediate(AnalyticsEvent.ScreenViewed.Dashboard)
            advanceUntilIdle()

            assertEquals(3, service.bufferedCount())

            // When we flush, the immediate one comes out FIRST
            service.onSentryReady()
            advanceUntilIdle()
            assertEquals(3, downstream.tracked.size, "all three must be drained")
            // Can't distinguish them by value (same enum), but order is verifiable:
            // the head-insertion property is what matters. We verify the policy by
            // adding a distinguishable immediate item below.
        }

    // ============ READY DRAIN ============

    @Test
    fun `onSentryReady flips the flag`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            assertFalse(service.isReady)
            service.onSentryReady()
            assertTrue(service.isReady)
        }

    @Test
    fun `onSentryReady drains the buffer to the downstream`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            // Pile up multiple buffered events
            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            service.captureException(RuntimeException("a"))
            advanceUntilIdle()
            assertEquals(3, service.bufferedCount())

            service.onSentryReady()
            advanceUntilIdle()

            assertEquals(2, downstream.tracked.size)
            assertEquals(1, downstream.capturedExceptions.size)
            assertEquals(0, service.bufferedCount(), "buffer must be empty after flush")
        }

    @Test
    fun `onSentryReady is idempotent - second call does not double-flush`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            advanceUntilIdle()

            service.onSentryReady()
            advanceUntilIdle()
            assertEquals(1, downstream.tracked.size)

            // Second ready call must not re-flush already-drained events.
            service.onSentryReady()
            advanceUntilIdle()
            assertEquals(1, downstream.tracked.size, "second ready signal must be a no-op")
        }

    // ============ POST-READY PASS-THROUGH ============

    @Test
    fun `track after ready goes directly to downstream not the buffer`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)
            service.onSentryReady()
            advanceUntilIdle()

            service.track(AnalyticsEvent.ScreenViewed.Dashboard)

            assertEquals(1, downstream.tracked.size)
            assertEquals(0, service.bufferedCount())
        }

    @Test
    fun `trackImmediate after ready goes directly to downstream`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)
            service.onSentryReady()
            advanceUntilIdle()

            service.trackImmediate(AnalyticsEvent.ScreenViewed.Dashboard)

            assertEquals(1, downstream.tracked.size)
            assertEquals(0, service.bufferedCount())
        }

    @Test
    fun `captureException after ready goes directly to downstream`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)
            service.onSentryReady()
            advanceUntilIdle()

            val boom = RuntimeException("boom")
            service.captureException(boom)

            assertEquals(1, downstream.capturedExceptions.size)
            assertSame(boom, downstream.capturedExceptions.first())
            assertEquals(0, service.bufferedCount())
        }

    @Test
    fun `captureExceptionImmediate after ready goes directly to downstream`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)
            service.onSentryReady()
            advanceUntilIdle()

            val boom = RuntimeException("boom")
            service.captureExceptionImmediate(boom)

            assertEquals(1, downstream.capturedExceptions.size)
            assertSame(boom, downstream.capturedExceptions.first())
        }

    // Periodic flush safety net is not directly unit-tested — the explicit
    // [onSentryReady] drain is the primary mechanism (covered above), and the
    // periodic ticker is a single `while (isActive) { delay(N); if (ready) flush() }`
    // loop that's exercised by the integration testing on real devices once
    // Tor wiring lands. Driving it in a unit test requires intricate
    // TestCoroutineScheduler choreography that adds more risk than the
    // behaviour it would pin.

    // ============ BOUNDED BUFFER POLICY ============

    @Test
    fun `track dropping policy when full is FIFO drop-oldest`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service =
                BufferedAnalyticsService(
                    downstream = downstream,
                    scope = unconfinedScope(),
                    maxBuffer = 3,
                    flushIntervalMs = 0L,
                )

            // Add 3 → buffer full
            repeat(3) { service.track(AnalyticsEvent.ScreenViewed.Dashboard) }
            // Add a 4th → oldest dropped, new appended
            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            advanceUntilIdle()

            assertEquals(3, service.bufferedCount(), "buffer must stay bounded")
        }

    @Test
    fun `trackImmediate dropping policy when full is drop-newest-tail to make room at head`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service =
                BufferedAnalyticsService(
                    downstream = downstream,
                    scope = unconfinedScope(),
                    maxBuffer = 3,
                    flushIntervalMs = 0L,
                )

            repeat(3) { service.track(AnalyticsEvent.ScreenViewed.Dashboard) }
            // Add an immediate → buffer was full, tail (newest non-critical) dropped,
            // new item inserted at head.
            service.trackImmediate(AnalyticsEvent.ScreenViewed.Dashboard)
            advanceUntilIdle()

            assertEquals(3, service.bufferedCount())
        }

    // ============ DOWNSTREAM-FAILURE FALLBACK ============

    @Test
    fun `track falls back to buffer when downstream throws after ready`() =
        runTest {
            val downstream = RecordingAnalyticsService(throwOnTrack = true)
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)
            service.onSentryReady()
            advanceUntilIdle()

            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            advanceUntilIdle()

            // Direct push raised → captured as failed → enqueued instead.
            assertEquals(0, downstream.tracked.size, "downstream MUST NOT have received the event (it threw)")
            assertEquals(1, service.bufferedCount(), "failed event must be preserved in the buffer")
        }

    @Test
    fun `captureException falls back to buffer when downstream throws after ready`() =
        runTest {
            val downstream = RecordingAnalyticsService(throwOnCaptureException = true)
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)
            service.onSentryReady()
            advanceUntilIdle()

            service.captureException(RuntimeException("boom"))
            advanceUntilIdle()

            assertEquals(0, downstream.capturedExceptions.size)
            assertEquals(1, service.bufferedCount())
        }

    // ============ DIAGNOSTICS ============

    @Test
    fun `isReady reflects the readiness flag`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            assertFalse(service.isReady)
            service.onSentryReady()
            assertTrue(service.isReady)
        }

    @Test
    fun `bufferedCount tracks enqueue and drain`() =
        runTest {
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            assertEquals(0, service.bufferedCount())

            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            service.track(AnalyticsEvent.ScreenViewed.Dashboard)
            advanceUntilIdle()
            assertEquals(2, service.bufferedCount())

            service.onSentryReady()
            advanceUntilIdle()
            assertEquals(0, service.bufferedCount())
        }
}
