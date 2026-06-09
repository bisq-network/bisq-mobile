package network.bisq.mobile.domain.analytics

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.utils.Logging

/**
 * In-memory buffering wrapper around an underlying [AnalyticsService] (typically
 * [SentryAnalyticsService]). Solves the problem that events fire from app
 * lifecycle / presenters at t=0, but the Sentry SDK can only be safely
 * initialised AFTER the Tor SOCKS port is available — which can be many
 * seconds into the bootstrap flow on a cold start, or never if the user is
 * connected via clearnet.
 *
 * Without a buffer those early events would be silently dropped. With one,
 * everything that happens between presenter wiring and Sentry-ready is
 * preserved and flushed when the transport becomes ready.
 *
 * ## Buffer policy
 *
 * - In-memory only — no disk persistence. Analytics events are low-value per
 *   item; a crash or kill that loses the buffer is acceptable (Sentry's own
 *   transport handles the post-init network-level retry).
 * - Bounded at [MAX_BUFFER] items. When full:
 *   - Normal [track] / [captureException]: drop the OLDEST queued item, append
 *     the new one to the tail (FIFO).
 *   - Immediate [trackImmediate] / [captureExceptionImmediate]: drop the
 *     NEWEST non-critical tail item, insert at the head — critical events
 *     jump the line.
 * - Flush triggers:
 *   - Explicit [onSentryReady] call from the lifecycle service — primary
 *     mechanism, drains immediately.
 *   - Periodic ticker every [FLUSH_INTERVAL_MS] — defence-in-depth in case
 *     [onSentryReady] is missed for any reason (e.g. a refactor that wires
 *     the lifecycle hook incorrectly). Cheap when not ready.
 *
 * ## Thread safety
 *
 * - [sentryReady]: `@Volatile` via atomicfu — single read / single write,
 *   happens-before published by [onSentryReady].
 * - The internal buffer is guarded by a [Mutex]. The lock is held only for
 *   in-memory operations, never around calls into the downstream Sentry SDK.
 * - The public [track] / [captureException] / [trackImmediate] /
 *   [captureExceptionImmediate] methods are non-suspending (matching the
 *   [AnalyticsService] contract). They take the fast path when Sentry is
 *   ready (Sentry SDKs are documented thread-safe) or fire-and-forget the
 *   buffer enqueue into [scope] when not — callers never block.
 *
 * ## Lifecycle
 *
 * The owning [scope] (typically `serviceScope` on `ApplicationLifecycleService`)
 * controls the periodic flusher and in-flight enqueues. Cancelling that scope
 * cancels both cleanly.
 *
 * @param downstream The SDK-backed service that ultimately ships events. In
 *  production this is [SentryAnalyticsService]; tests substitute a fake.
 * @param scope CoroutineScope used to run the periodic flusher and fire-and-
 *  forget enqueue operations. Caller owns lifecycle.
 * @param maxBuffer Upper bound on queued items. Defaults to [DEFAULT_MAX_BUFFER].
 * @param flushIntervalMs Cadence of the safety-net periodic flush check.
 *  Defaults to [DEFAULT_FLUSH_INTERVAL_MS].
 */
class BufferedAnalyticsService(
    private val downstream: AnalyticsService,
    private val scope: CoroutineScope,
    private val maxBuffer: Int = DEFAULT_MAX_BUFFER,
    private val flushIntervalMs: Long = DEFAULT_FLUSH_INTERVAL_MS,
) : AnalyticsService,
    Logging {
    private val buffer = ArrayDeque<BufferedItem>()
    private val mutex = Mutex()

    // atomicfu's atomic<Boolean> gives us cross-platform volatile semantics
    // without needing platform-specific @Volatile.
    private val sentryReady = atomic(false)

    private sealed class BufferedItem {
        data class Track(
            val event: AnalyticsEvent,
        ) : BufferedItem()

        data class Exception(
            val t: Throwable,
        ) : BufferedItem()
    }

    init {
        // Defence-in-depth periodic flush. Cheap when sentryReady=false (the
        // outer check skips lock acquisition + drain entirely). Primary
        // trigger is the explicit onSentryReady() call from the lifecycle.
        //
        // A non-positive flushIntervalMs disables the periodic safety net
        // entirely — used by tests that don't want an infinite delay loop
        // interfering with `advanceUntilIdle()`. Production code never sets
        // this to zero.
        if (flushIntervalMs > 0) {
            scope.launch {
                while (isActive) {
                    delay(flushIntervalMs)
                    if (sentryReady.value) flush()
                }
            }
        }
    }

    override fun init(
        dsn: String,
        environment: String,
        release: String,
        isDebug: Boolean,
        socksProxyHost: String?,
        socksProxyPort: Int?,
    ) {
        // Forward init synchronously. Readiness is a separate signal that the
        // lifecycle service flips via onSentryReady() once Tor is up AND
        // Sentry.init has completed — see ApplicationLifecycleService.
        downstream.init(dsn, environment, release, isDebug, socksProxyHost, socksProxyPort)
    }

    override fun track(event: AnalyticsEvent) {
        if (sentryReady.value && tryDirect { downstream.track(event) }) {
            log.d { "Analytics: track(${event.name}) → forwarded direct" }
            return
        }
        // Not ready, or direct push threw: buffer at TAIL (FIFO ordering).
        log.d { "Analytics: track(${event.name}) → buffered (Sentry not ready)" }
        scope.launch { enqueueAtTail(BufferedItem.Track(event)) }
    }

    override fun trackImmediate(event: AnalyticsEvent) {
        if (sentryReady.value && tryDirect { downstream.trackImmediate(event) }) {
            log.d { "Analytics: trackImmediate(${event.name}) → forwarded direct" }
            return
        }
        // Not ready, or direct push threw: buffer at HEAD so this jumps any
        // lower-priority events still queued behind it.
        log.d { "Analytics: trackImmediate(${event.name}) → buffered at HEAD (Sentry not ready)" }
        scope.launch { enqueueAtHead(BufferedItem.Track(event)) }
    }

    override fun captureException(throwable: Throwable) {
        if (sentryReady.value && tryDirect { downstream.captureException(throwable) }) {
            log.d { "Analytics: captureException(${throwable::class.simpleName}) → forwarded direct" }
            return
        }
        log.d { "Analytics: captureException(${throwable::class.simpleName}) → buffered (Sentry not ready)" }
        scope.launch { enqueueAtTail(BufferedItem.Exception(throwable)) }
    }

    override fun captureExceptionImmediate(throwable: Throwable) {
        if (sentryReady.value && tryDirect { downstream.captureExceptionImmediate(throwable) }) {
            log.d { "Analytics: captureExceptionImmediate(${throwable::class.simpleName}) → forwarded direct" }
            return
        }
        log.d { "Analytics: captureExceptionImmediate(${throwable::class.simpleName}) → buffered at HEAD (Sentry not ready)" }
        scope.launch { enqueueAtHead(BufferedItem.Exception(throwable)) }
    }

    /**
     * Signal that the downstream Sentry SDK is fully initialised and the
     * transport is ready to accept events. Idempotent — subsequent calls are
     * no-ops. Triggers an immediate buffer drain on the owning [scope].
     *
     * Called by `ApplicationLifecycleService` once analytics init has
     * succeeded AND (in the next PR) the Tor SOCKS port is wired into the
     * SDK options.
     */
    fun onSentryReady() {
        if (!sentryReady.compareAndSet(expect = false, update = true)) return
        log.d { "Sentry ready — flushing buffered analytics events" }
        scope.launch { flush() }
    }

    /**
     * Current buffer occupancy. Exposed for diagnostics + tests; production
     * code shouldn't make decisions on this.
     */
    suspend fun bufferedCount(): Int = mutex.withLock { buffer.size }

    /**
     * Whether the readiness flag has been flipped. Exposed for diagnostics + tests.
     */
    val isReady: Boolean get() = sentryReady.value

    // ============ Internals ============

    /**
     * Run the SDK call inside a swallowing try/catch. Returns true on success,
     * false if the SDK call threw (in which case the caller falls back to
     * buffering). Cancellation is always propagated — never swallowed.
     */
    private inline fun tryDirect(block: () -> Unit): Boolean =
        try {
            block()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            log.w(t) { "Direct analytics push failed — falling back to buffer" }
            false
        }

    private suspend fun enqueueAtTail(item: BufferedItem) =
        mutex.withLock {
            if (buffer.size >= maxBuffer) buffer.removeFirst() // drop oldest
            buffer.addLast(item)
        }

    private suspend fun enqueueAtHead(item: BufferedItem) =
        mutex.withLock {
            // Critical event wins the slot — drop a tail (lower-priority) item.
            if (buffer.size >= maxBuffer) buffer.removeLast()
            buffer.addFirst(item)
        }

    /**
     * Snapshot the buffer under lock, then dispatch downstream OUTSIDE the
     * lock. Never holds the mutex during a SDK call — that would let a slow
     * SDK call block all incoming enqueues.
     *
     * A single item that throws during send is dropped (logged but not
     * re-enqueued) to avoid a hot-loop on permanently-bad payloads. Sentry's
     * transport handles retry for transient network failures via its own
     * internal queue.
     */
    private suspend fun flush() {
        val snapshot =
            mutex.withLock {
                if (buffer.isEmpty()) return
                val items = buffer.toList()
                buffer.clear()
                items
            }
        log.d { "Flushing ${snapshot.size} buffered analytics events" }
        snapshot.forEach { item ->
            tryDirect {
                when (item) {
                    is BufferedItem.Track -> downstream.track(item.event)
                    is BufferedItem.Exception -> downstream.captureException(item.t)
                }
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_BUFFER = 200
        const val DEFAULT_FLUSH_INTERVAL_MS = 5_000L
    }
}
