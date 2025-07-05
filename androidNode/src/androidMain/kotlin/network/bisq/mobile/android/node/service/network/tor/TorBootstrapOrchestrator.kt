package network.bisq.mobile.android.node.service.network.tor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CancellationException
import network.bisq.mobile.domain.utils.Logging

/**
 * Orchestrates Tor bootstrap process for Bisq Mobile
 * Handles initialization, state monitoring, and coordination with application bootstrap
 */
class TorBootstrapOrchestrator(
    private val torIntegrationService: TorIntegrationService
) : Logging {

    private val torBootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var torMonitoringJob: Job? = null
    private var torBootstrapComplete = CompletableDeferred<Boolean>()

    interface TorBootstrapCallback {
        fun onTorStateChanged(message: String, progress: Float)
        fun onTorReady(socksPort: Int)
        fun onTorTimeout()
        fun onTorError(exception: Exception)
    }

    fun initializeAndWaitForTor(
        callback: TorBootstrapCallback,
        jobsManager: network.bisq.mobile.domain.utils.CoroutineJobsManager
    ) {
        log.i { "🚀 Bootstrap: Initializing embedded Tor daemon and waiting for ready state..." }

        callback.onTorStateChanged("Initializing Tor daemon...", 0.05f)

        setupTorStateObserver(callback, jobsManager)

        torMonitoringJob = torBootstrapScope.launch {
            try {
                torIntegrationService.initializeAndStart(
                    maxRetries = 3,
                    retryDelayMs = 5000
                )

                log.i { "✅ Bootstrap: Tor initialization started - waiting for ready state..." }
                callback.onTorStateChanged("Starting Tor daemon...", 0.1f)

                val statusCheckJob = launch {
                    repeat(12) {
                        delay(5000)
                        val currentState = torIntegrationService.torState.value
                        val currentPort = torIntegrationService.socksPort.value
                        log.i { "🔍 Bootstrap: Tor status check - State: $currentState, Port: $currentPort" }

                        when (currentState) {
                            TorService.TorState.STARTING -> callback.onTorStateChanged("Starting Tor daemon...", 0.1f + (it * 0.1f))
                            TorService.TorState.BOOTSTRAPPING -> callback.onTorStateChanged("Tor connecting to network...", 0.15f + (it * 0.08f))
                            TorService.TorState.READY -> {
                                if (currentPort != null) {
                                    callback.onTorStateChanged("Tor ready - Starting Bisq...", 0.25f)
                                } else {
                                    callback.onTorStateChanged("Tor almost ready...", 0.2f + (it * 0.04f))
                                }
                            }
                            else -> { }
                        }
                    }
                }

                val torReady = withTimeoutOrNull(60000) {
                    torBootstrapComplete.await()
                }

                statusCheckJob.cancel()

                if (torReady == true) {
                    log.i { "🚀 Bootstrap: Tor is ready - proceeding with application bootstrap" }
                    callback.onTorStateChanged("Tor ready - Starting Bisq...", 0.25f)
                    delay(1000)

                    val socksPort = torIntegrationService.socksPort.value
                    if (socksPort != null) {
                        callback.onTorReady(socksPort)
                    } else {
                        log.e { "❌ Bootstrap: Tor ready but no SOCKS port available!" }
                        callback.onTorError(RuntimeException("Tor ready but no SOCKS port available"))
                    }
                } else {
                    log.w { "⚠️ Bootstrap: Tor timeout after 60 seconds" }
                    handleTorTimeout(callback)
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    log.d { "🔄 Bootstrap: Tor initialization cancelled (normal during deactivation)" }
                    return@launch
                }
                log.e(e) { "❌ Bootstrap: Failed to start Tor initialization" }
                callback.onTorError(e)
            }
        }
    }

    private suspend fun handleTorTimeout(callback: TorBootstrapCallback) {
        callback.onTorStateChanged("Tor timeout - Debugging...", 0.2f)

        torIntegrationService.debugAndFixTorStatus()
        delay(2000)

        val finalState = torIntegrationService.torState.value
        val finalPort = torIntegrationService.socksPort.value
        log.w { "⚠️ Bootstrap: Final Tor status after debug - State: $finalState, Port: $finalPort" }

        if (finalState == TorService.TorState.READY && finalPort != null) {
            log.i { "🚀 Bootstrap: Tor was actually ready after debug - proceeding with bootstrap" }
            callback.onTorStateChanged("Tor ready - Starting Bisq...", 0.25f)
            delay(1000)
            callback.onTorReady(finalPort)
        } else {
            callback.onTorTimeout()
        }
    }

    private fun setupTorStateObserver(
        callback: TorBootstrapCallback,
        jobsManager: network.bisq.mobile.domain.utils.CoroutineJobsManager
    ) {
        jobsManager.addJob(torBootstrapScope.launch {
            try {
                var shouldContinue = true

                torIntegrationService.torState.collect { torState ->
                    if (!shouldContinue) return@collect

                    log.i { "🔍 Bootstrap: Tor state changed to: $torState" }

                    when (torState) {
                        TorService.TorState.STARTING -> {
                            callback.onTorStateChanged("Starting Tor daemon...", 0.1f)
                        }
                        TorService.TorState.BOOTSTRAPPING -> {
                            callback.onTorStateChanged("Tor connecting to network...", 0.15f)
                        }
                        TorService.TorState.READY -> {
                            checkTorReadiness()
                            shouldContinue = false
                        }
                        TorService.TorState.ERROR -> {
                            log.e { "❌ Bootstrap: Tor encountered an error" }
                            if (!torBootstrapComplete.isCompleted) {
                                torBootstrapComplete.complete(false)
                            }
                            shouldContinue = false
                        }
                        else -> {
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    log.d { "🔄 Bootstrap: Tor state monitoring cancelled (normal during deactivation)" }
                } else {
                    log.e(e) { "❌ Bootstrap: Error in Tor state observer" }
                    if (!torBootstrapComplete.isCompleted) {
                        torBootstrapComplete.complete(false)
                    }
                }
            }
        })

        jobsManager.addJob(torBootstrapScope.launch {
            try {
                torIntegrationService.socksPort.collect { socksPort ->
                    log.i { "🔍 Bootstrap: SOCKS port changed to: $socksPort" }

                    if (socksPort != null && torIntegrationService.torState.value == TorService.TorState.READY) {
                        checkTorReadiness()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    log.d { "🔄 Bootstrap: SOCKS port monitoring cancelled (normal during deactivation)" }
                } else {
                    log.e(e) { "❌ Bootstrap: Error in SOCKS port observer" }
                }
            }
        })
    }

    /**
     * Check if Tor is fully ready (both state READY and SOCKS port available)
     */
    private fun checkTorReadiness() {
        val torState = torIntegrationService.torState.value
        val socksPort = torIntegrationService.socksPort.value

        log.i { "🔍 Bootstrap: Checking Tor readiness - State: $torState, SOCKS Port: $socksPort" }

        if (torState == TorService.TorState.READY && socksPort != null) {
            log.i { "🚀 Bootstrap: Tor fully ready with SOCKS port: $socksPort" }

            if (!torBootstrapComplete.isCompleted) {
                torBootstrapComplete.complete(true)
            }
        } else {
            log.i { "⏳ Bootstrap: Tor not fully ready yet - waiting for both READY state and SOCKS port" }
        }
    }

    fun cancelTorMonitoring(bootstrapSuccessful: Boolean) {
        if (torBootstrapComplete.isCompleted) {
            if (!bootstrapSuccessful) {
                log.w { "⚠️ Bootstrap failed - cancelling Tor monitoring" }
                torMonitoringJob?.cancel()
                torMonitoringJob = null

                if (!torBootstrapComplete.isCompleted) {
                    torBootstrapComplete.cancel()
                }
                torBootstrapComplete = CompletableDeferred()
            } else {
                log.i { "✅ Bootstrap successful - keeping Tor monitoring active" }
            }
        } else {
            log.w { "⚠️ Bootstrap not completed - skipping cancellation of Tor monitoring" }
        }
    }
}
