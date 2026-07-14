package network.bisq.mobile.data.service.network

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.data.service.LifeCycleAware
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging
import kotlin.coroutines.cancellation.CancellationException

abstract class NetworkServiceFacade(
    private val kmpTorService: KmpTorService,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
) : ServiceFacade(),
    LifeCycleAware,
    Logging {
    companion object {
        // Bounded retry for the daemon start: a stale control-auth cookie fails the first
        // AUTHENTICATE, and the per-attempt cleanup in KmpTorService.startTor() clears it so a
        // subsequent attempt succeeds. Kept small — genuine failures should surface, not loop.
        private const val MAX_TOR_START_ATTEMPTS = 3
        private const val TOR_START_RETRY_DELAY_MS = 1_000L

        // Upper bound for awaiting Started / torBootstrapFailed after [startTorWithRetries].
        // Aligns with kmp-tor per-attempt daemon + bootstrap caps in [KmpTorService.startTor].
        private const val TOR_ACTIVATION_AWAIT_TIMEOUT_MS =
            KmpTorService.DEFAULT_DAEMON_START_TIMEOUT_MS + KmpTorService.DEFAULT_BOOTSTRAP_TIMEOUT_MS
    }

    abstract val numConnections: StateFlow<Int>
    abstract val allDataReceived: StateFlow<Boolean>

    abstract suspend fun isTorEnabled(): Boolean

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        if (isTorEnabled()) {
            startTorWithRetries()
            awaitTorStartedOrBootstrapFailed()
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
            if (applicationBootstrapFacade.torBootstrapFailed.value) {
                log.i { "Tor bootstrap failed (terminal); skipping remaining start attempts" }
                return
            }
            currentCoroutineContext().ensureActive()

            val started =
                try {
                    kmpTorService.startTor()
                } catch (e: Exception) {
                    log.e(e) { "Tor service failed to start (attempt ${attempt + 1}/$MAX_TOR_START_ATTEMPTS)" }
                    currentCoroutineContext().ensureActive()
                    false
                }
            if (started) return

            if (applicationBootstrapFacade.torBootstrapFailed.value) {
                log.i {
                    "Tor bootstrap failed (terminal) after attempt ${attempt + 1}/$MAX_TOR_START_ATTEMPTS; " +
                        "skipping remaining retries"
                }
                return
            }

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

    private suspend fun awaitTorStartedOrBootstrapFailed() {
        if (kmpTorService.state.value is KmpTorService.TorState.Started) {
            return
        }

        val torStarted =
            kmpTorService.state
                .filter { it is KmpTorService.TorState.Started }
                .map { TorActivationOutcome.Started }

        val torFailed =
            applicationBootstrapFacade.torBootstrapFailed
                .filter { it }
                .map { TorActivationOutcome.BootstrapFailed }

        try {
            when (
                withTimeout(TOR_ACTIVATION_AWAIT_TIMEOUT_MS) {
                    merge(torStarted, torFailed).first()
                }
            ) {
                TorActivationOutcome.Started -> Unit

                TorActivationOutcome.BootstrapFailed -> handleTorBootstrapNotReady()
            }
        } catch (_: TimeoutCancellationException) {
            log.w {
                "Timed out after ${TOR_ACTIVATION_AWAIT_TIMEOUT_MS}ms waiting for Tor started " +
                    "or bootstrap failure; treating as not ready"
            }
            handleTorBootstrapNotReady()
        }
    }

    private suspend fun handleTorBootstrapNotReady() {
        try {
            deactivate()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Failed to deactivate after Tor bootstrap failure" }
        }
        throw TorBootstrapNotReadyException()
    }

    private sealed interface TorActivationOutcome {
        data object Started : TorActivationOutcome

        data object BootstrapFailed : TorActivationOutcome
    }
}
