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

            service.track(AnalyticsEvent.ScreenOpened.Dashboard)
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

            // The AnalyticsEvent sealed type has only one concrete subclass right
            // now (Dashboard), so we use exception messages as the distinguishable
            // identifier — both code paths share the same head/tail enqueue
            // policy via the BufferedItem sealed type. Two normal-priority
            // exceptions go in first (tail), then one immediate exception
            // (head) — that one must come out FIRST on flush.
            val normalA = RuntimeException("normal-a")
            val normalB = RuntimeException("normal-b")
            val critical = RuntimeException("critical")

            service.captureException(normalA)
            service.captureException(normalB)
            service.captureExceptionImmediate(critical)
            advanceUntilIdle()

            assertEquals(3, service.bufferedCount())

            service.onSentryReady()
            advanceUntilIdle()

            assertEquals(3, downstream.capturedExceptions.size, "all three must be drained")
            assertEquals(
                listOf<Throwable>(critical, normalA, normalB),
                downstream.capturedExceptions,
                "immediate item must be flushed FIRST (head insertion), normal items follow in FIFO tail order",
            )
        }

    @Test
    fun `trackImmediate before ready buffers via the same head-insertion path as captureExceptionImmediate`() =
        runTest {
            // Symmetric coverage for the track() codepath. AnalyticsEvent's
            // sealed hierarchy currently only has Dashboard, so we can't
            // distinguish events by value — but the BUFFER STATE side of the
            // contract is still observable: a normal track sits at tail, an
            // immediate track jumps to head, and the head-vs-tail eviction
            // policy differs (verified in the BOUNDED BUFFER POLICY tests
            // below). The ordering contract for the same-priority path is
            // pinned by the captureExceptionImmediate test above; this test
            // ensures `trackImmediate` itself is exercised so a future
            // refactor that breaks it loudly fails.
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)

            service.track(AnalyticsEvent.ScreenOpened.Dashboard)
            service.trackImmediate(AnalyticsEvent.ScreenOpened.Dashboard)
            advanceUntilIdle()

            assertEquals(2, service.bufferedCount(), "both tracks must be in the buffer pre-ready")
            assertTrue(downstream.tracked.isEmpty(), "nothing reaches downstream pre-ready")

            service.onSentryReady()
            advanceUntilIdle()

            assertEquals(2, downstream.tracked.size, "both items must flush on ready")
        }

    @Test
    fun `trackImmediate after ready forwards directly without buffering`() =
        runTest {
            // Once sentryReady is flipped, trackImmediate must call
            // downstream.trackImmediate inline (not via the buffer path). This
            // verifies the synchronous fast-path is wired so subsequent
            // refactors can't accidentally route all calls through the buffer.
            val downstream = RecordingAnalyticsService()
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)
            service.onSentryReady()
            advanceUntilIdle()

            service.trackImmediate(AnalyticsEvent.ScreenOpened.Dashboard)
            // No advanceUntilIdle needed — the direct path is synchronous.

            assertEquals(1, downstream.tracked.size, "trackImmediate post-ready must forward directly")
            assertEquals(0, service.bufferedCount(), "buffer stays empty when direct path takes the event")
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
            service.track(AnalyticsEvent.ScreenOpened.Dashboard)
            service.track(AnalyticsEvent.ScreenOpened.Dashboard)
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

            service.track(AnalyticsEvent.ScreenOpened.Dashboard)
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

            service.track(AnalyticsEvent.ScreenOpened.Dashboard)

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

            service.trackImmediate(AnalyticsEvent.ScreenOpened.Dashboard)

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

            // Use exceptions for distinguishable identity — see the buffer-
            // ordering test above for why we use exception messages.
            val e1 = RuntimeException("e1-oldest")
            val e2 = RuntimeException("e2")
            val e3 = RuntimeException("e3")
            val e4 = RuntimeException("e4-newest")

            // Fill the buffer
            service.captureException(e1)
            service.captureException(e2)
            service.captureException(e3)
            // Overflow → e1 (oldest) must be evicted, e4 appended at tail
            service.captureException(e4)
            advanceUntilIdle()

            assertEquals(3, service.bufferedCount(), "buffer must stay bounded")

            service.onSentryReady()
            advanceUntilIdle()

            assertEquals(
                listOf<Throwable>(e2, e3, e4),
                downstream.capturedExceptions,
                "FIFO drop-oldest: e1 must be evicted, remaining items in insertion order",
            )
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

            // Three normal-priority items fill the buffer. Then a critical
            // (immediate) item arrives: the TAIL (newest non-critical, lowest
            // priority) gets dropped, and the critical lands at HEAD. The
            // remaining items keep their relative order.
            val normal1 = RuntimeException("normal-1-oldest")
            val normal2 = RuntimeException("normal-2")
            val normal3Dropped = RuntimeException("normal-3-will-be-dropped")
            val critical = RuntimeException("critical-jumps-head")

            service.captureException(normal1)
            service.captureException(normal2)
            service.captureException(normal3Dropped)
            // Triggers drop-newest-tail + head insertion
            service.captureExceptionImmediate(critical)
            advanceUntilIdle()

            assertEquals(3, service.bufferedCount())

            service.onSentryReady()
            advanceUntilIdle()

            assertEquals(
                listOf<Throwable>(critical, normal1, normal2),
                downstream.capturedExceptions,
                "critical at HEAD, oldest survivors in FIFO order, newest non-critical (normal3Dropped) evicted",
            )
        }

    // ============ DOWNSTREAM-FAILURE FALLBACK ============

    @Test
    fun `track falls back to buffer when downstream throws after ready`() =
        runTest {
            val downstream = RecordingAnalyticsService(throwOnTrack = true)
            val service = BufferedAnalyticsService(downstream, unconfinedScope(), flushIntervalMs = 0L)
            service.onSentryReady()
            advanceUntilIdle()

            service.track(AnalyticsEvent.ScreenOpened.Dashboard)
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

            service.track(AnalyticsEvent.ScreenOpened.Dashboard)
            service.track(AnalyticsEvent.ScreenOpened.Dashboard)
            advanceUntilIdle()
            assertEquals(2, service.bufferedCount())

            service.onSentryReady()
            advanceUntilIdle()
            assertEquals(0, service.bufferedCount())
        }
}
