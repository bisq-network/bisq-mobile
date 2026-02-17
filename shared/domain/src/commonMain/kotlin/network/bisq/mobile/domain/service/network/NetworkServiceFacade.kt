package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

abstract class NetworkServiceFacade(
    private val kmpTorService: KmpTorService,
) : ServiceFacade(),
    LifeCycleAware,
    Logging {
    abstract val numConnections: StateFlow<Int>
    abstract val allDataReceived: StateFlow<Boolean>

    abstract suspend fun isTorEnabled(): Boolean

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        if (isTorEnabled()) {
            try {
                kmpTorService.startTor()
            } catch (e: Exception) {
                log.e(e) { "Tor service failed to start" }
                // ApplicationBootstrapFacade observes tor state & triggers dialog in SplashPresenter
                currentCoroutineContext().ensureActive()
            }
            // suspend until tor is started
            // this is fine because:
            // - we restart tor till it starts
            // - or close the app otherwise
            kmpTorService.state.filter { it is KmpTorService.TorState.Started }.first()
        }
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
