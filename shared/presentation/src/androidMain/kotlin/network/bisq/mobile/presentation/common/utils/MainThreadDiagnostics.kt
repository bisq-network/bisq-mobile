package network.bisq.mobile.presentation.common.utils

import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.SystemClock
import android.util.Log
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Debug-only instrumentation. Three complementary detectors, all logging to logcat:
 *
 * 1. StrictMode thread policy — names disk/network calls made on the main thread (`StrictMode` tag).
 * 2. Looper slow-dispatch log — names the exact Handler/message whose dispatch exceeded a frame
 *    budget (tag [TAG], "Slow main dispatch").
 * 3. Pulse watchdog — a background thread that expects a 50ms heartbeat from the main looper and,
 *    when the heartbeat goes silent, samples the main thread's live stack *mid-stall* (tag [TAG],
 *    "Main thread stalled"). This catches blockage from lock contention/computation that neither
 *    StrictMode nor the dispatch log attributes.
 *
 * Correlate findings by timestamp: a dropped frame shows up as a stalled pulse with the culprit
 * stack, usually accompanied by either a StrictMode violation or a slow-dispatch line naming the
 * message. Watchdog self-disables after [WATCHDOG_LIFETIME_MS] — bootstrap is the window of
 * interest and the sampler should not spam logs for a whole session.
 *
 * Coverage-excluded: debug-only instrumentation wired to Looper/StrictMode is not unit-testable.
 * The pure sanitizer logic IS covered via MainThreadDiagnosticsSanitizerTest.
 */
@ExcludeFromCoverage
object MainThreadDiagnostics {
    private const val TAG = "MainThreadPulse"
    private const val HEARTBEAT_INTERVAL_MS = 50L
    private const val STALL_THRESHOLD_MS = 100L
    private const val SLOW_DISPATCH_THRESHOLD_MS = 32L
    private const val WATCHDOG_LIFETIME_MS = 5 * 60 * 1000L
    private const val MAX_SAMPLES_PER_STALL = 5

    @Volatile
    private var installed = false

    /** Must be called from the main thread (Application.onCreate). No-op unless [isDebug]. */
    fun install(isDebug: Boolean) {
        if (!isDebug || installed) return
        installed = true
        installStrictMode()
        installSlowDispatchLog()
        startPulseWatchdog()
        Log.i(TAG, "Main-thread diagnostics installed")
    }

    private fun installStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
    }

    // The raw Looper dispatch line embeds the callback's toString(), which for a custom
    // Runnable could carry arbitrary (sensitive) state — and a dotted token in it can be a
    // hostname or address just as easily as a class name, so free-form token harvesting is
    // out. Looper builds the line as ">>>>> Dispatching to <Handler.toString()> <callback>:
    // <what>", and Handler.toString() is framework-generated ("Handler (<class>) {<hex>}"),
    // so an anchored structural match yields the handler class and `what` from trusted
    // metadata only. From the callback segment we keep just two framework-generated shapes:
    // a default Object.toString() (Class@hex) and kotlinx.coroutines' "Continuation at
    // <fqcn>" (sourced from a StackTraceElement) — anything else collapses to a placeholder.
    private val DISPATCH_LINE = Regex("""^>>>>> Dispatching to Handler \(([\w.$]+)\) \{[0-9a-f]+\} (.*): (-?\d+)$""")
    private val DEFAULT_TO_STRING = Regex("""^([\w.$]+)@[0-9a-f]+$""")
    private val CONTINUATION_AT = Regex("""Continuation at ([\w.$]+)""")

    internal fun sanitizeDispatchTarget(line: String?): String {
        val match = line?.let { DISPATCH_LINE.find(it) } ?: return "unknown"
        val (handlerClass, callback, what) = match.destructured
        val callbackPart =
            when {
                callback == "null" -> null
                else ->
                    DEFAULT_TO_STRING.find(callback)?.groupValues?.get(1)
                        ?: CONTINUATION_AT
                            .findAll(callback)
                            .map { it.groupValues[1] }
                            .distinct()
                            .joinToString(" ")
                            .ifEmpty { "custom-callback" }
            }
        return buildString {
            append(handlerClass)
            if (callbackPart != null) append(' ').append(callbackPart)
            append(" what=").append(what)
        }
    }

    private fun installSlowDispatchLog() {
        var dispatchStartMs = 0L
        var currentMessage: String? = null
        Looper.getMainLooper().setMessageLogging { line ->
            if (line.startsWith(">>>>> Dispatching")) {
                dispatchStartMs = SystemClock.uptimeMillis()
                currentMessage = line
            } else if (line.startsWith("<<<<< Finished")) {
                val tookMs = SystemClock.uptimeMillis() - dispatchStartMs
                if (tookMs >= SLOW_DISPATCH_THRESHOLD_MS) {
                    Log.w(TAG, "Slow main dispatch ${tookMs}ms: ${sanitizeDispatchTarget(currentMessage)}")
                }
            }
        }
    }

    private fun startPulseWatchdog() {
        val lastBeatMs = AtomicLong(SystemClock.uptimeMillis())
        val mainHandler = Handler(Looper.getMainLooper())
        val heartbeat =
            object : Runnable {
                override fun run() {
                    lastBeatMs.set(SystemClock.uptimeMillis())
                    mainHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
                }
            }
        mainHandler.post(heartbeat)

        val mainThread = Looper.getMainLooper().thread
        val deadline = SystemClock.uptimeMillis() + WATCHDOG_LIFETIME_MS
        thread(name = "MainThreadPulseWatchdog", isDaemon = true) {
            var samplesThisStall = 0
            while (SystemClock.uptimeMillis() < deadline) {
                Thread.sleep(HEARTBEAT_INTERVAL_MS)
                val silentForMs = SystemClock.uptimeMillis() - lastBeatMs.get()
                if (silentForMs >= STALL_THRESHOLD_MS) {
                    if (samplesThisStall < MAX_SAMPLES_PER_STALL) {
                        samplesThisStall++
                        // Sample while the stall is ongoing — this stack IS the culprit (or its tail).
                        val stack = mainThread.stackTrace.joinToString(separator = "\n    ") { it.toString() }
                        Log.w(TAG, "Main thread stalled ~${silentForMs}ms; main stack:\n    $stack")
                    }
                    // Back off so a single long stall produces a few samples, not hundreds.
                    Thread.sleep(200)
                } else {
                    samplesThisStall = 0
                }
            }
            // Removal must run on the main looper: the heartbeat always re-posts itself before
            // returning, so by the time this queued task executes the next beat is in the queue
            // and gets removed. Removing directly from this thread could interleave between
            // lastBeatMs.set(...) and postDelayed(...), leaving a beat re-queued forever.
            mainHandler.post { mainHandler.removeCallbacks(heartbeat) }
            Log.i(TAG, "Pulse watchdog finished its ${WATCHDOG_LIFETIME_MS / 60000}min window")
        }
    }
}
