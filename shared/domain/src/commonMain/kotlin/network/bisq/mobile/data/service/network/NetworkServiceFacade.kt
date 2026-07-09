package network.bisq.mobile.data.service.network

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.service.LifeCycleAware
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

abstract class NetworkServiceFacade(
    private val kmpTorService: KmpTorService,
) : ServiceFacade(),
    LifeCycleAware,
    Logging {
    companion object {
        // Bounded retry for the daemon start: a stale control-auth cookie fails the first
        // AUTHENTICATE, and the per-attempt cleanup in KmpTorService.startTor() clears it so a
        // subsequent attempt succeeds. Kept small — genuine failures should surface, not loop.
        private const val MAX_TOR_START_ATTEMPTS = 3
        private const val TOR_START_RETRY_DELAY_MS = 1_000L
    }

    abstract val numConnections: StateFlow<Int>
    abstract val allDataReceived: StateFlow<Boolean>

    abstract suspend fun isTorEnabled(): Boolean

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        if (isTorEnabled()) {
            startTorWithRetries()
        }
    }

    /**
     * Starts Tor, retrying a bounded number of times, and returns once it is started or all attempts
     * are exhausted. Each [KmpTorService.startTor] attempt first clears stale control-plane files, so
     * a leftover control-auth cookie that fails AUTHENTICATE ("515 Authentication cookie did not
     * match expected value") on one attempt is gone by the next.
     *
     * We rely on [KmpTorService.startTor]'s own return value (it already suspends until bootstrapped
     * or failed). The previous implementation additionally waited indefinitely for
     * [KmpTorService.TorState.Started], which hung forever — splash frozen at "Starting Tor 0%" —
     * when the daemon failed to start. If all attempts fail we return rather than hang;
     * ApplicationBootstrapFacade observes the Tor state and its stage timeout surfaces the failure
     * dialog / retry to the user.
     */
    private suspend fun startTorWithRetries() {
        repeat(MAX_TOR_START_ATTEMPTS) { attempt ->
            val started =
                try {
                    kmpTorService.startTor()
                } catch (e: Exception) {
                    log.e(e) { "Tor service failed to start (attempt ${attempt + 1}/$MAX_TOR_START_ATTEMPTS)" }
                    currentCoroutineContext().ensureActive()
                    false
                }
            if (started) return
            if (attempt < MAX_TOR_START_ATTEMPTS - 1) {
                log.w { "Tor did not start (attempt ${attempt + 1}/$MAX_TOR_START_ATTEMPTS); retrying in ${TOR_START_RETRY_DELAY_MS}ms" }
                delay(TOR_START_RETRY_DELAY_MS)
            }
        }
        log.e { "Tor failed to start after $MAX_TOR_START_ATTEMPTS attempts; bootstrap will surface the failure" }
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()

        if (isTorEnabled()) {
            kmpTorService.stopTor()
        }
    }

    /**
     * Restarts the Tor daemon if it was stopped (e.g. iOS killed it while backgrounded).
     * Safe to call when Tor is already running — returns immediately.
     * Failures are caught and logged to prevent crashes.
     */
    suspend fun ensureTorRunning() {
        if (isTorEnabled() && kmpTorService.state.value is KmpTorService.TorState.Stopped) {
            log.i { "Tor is stopped, attempting restart..." }
            try {
                kmpTorService.startTor()
            } catch (e: Exception) {
                log.e(e) { "Failed to start Tor while ensuring it's running" }
                currentCoroutineContext().ensureActive()
            }
        }
    }
}
