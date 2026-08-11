package network.bisq.mobile.presentation.common.utils

import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.SystemClock
import android.util.Log
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
 */
object MainThreadDiagnostics {
    private const val TAG = "MainThreadPulse"
    private const val HEARTBEAT_INTERVAL_MS = 50L
    private const val STALL_THRESHOLD_MS = 100L
    private const val SLOW_DISPATCH_THRESHOLD_MS = 32L
    private const val WATCHDOG_LIFETIME_MS = 5 * 60 * 1000L

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
                    Log.w(TAG, "Slow main dispatch ${tookMs}ms: ${currentMessage?.removePrefix(">>>>> Dispatching to ")}")
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
            while (SystemClock.uptimeMillis() < deadline) {
                Thread.sleep(HEARTBEAT_INTERVAL_MS)
                val silentForMs = SystemClock.uptimeMillis() - lastBeatMs.get()
                if (silentForMs >= STALL_THRESHOLD_MS) {
                    // Sample while the stall is ongoing — this stack IS the culprit (or its tail).
                    val stack = mainThread.stackTrace.joinToString(separator = "\n    ") { it.toString() }
                    Log.w(TAG, "Main thread stalled ~${silentForMs}ms; main stack:\n    $stack")
                    // Back off so a single long stall produces a few samples, not hundreds.
                    Thread.sleep(200)
                }
            }
            mainHandler.removeCallbacks(heartbeat)
            Log.i(TAG, "Pulse watchdog finished its ${WATCHDOG_LIFETIME_MS / 60000}min window")
        }
    }
}
