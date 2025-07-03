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
    
    // Callback interfaces for communicating with the bootstrap facade
    interface TorBootstrapCallback {
        fun onTorStateChanged(message: String, progress: Float)
        fun onTorReady(socksPort: Int)
        fun onTorTimeout()
        fun onTorError(exception: Exception)
    }

    /**
     * Initialize Tor and wait for it to be ready before proceeding with bootstrap
     * This ensures Tor is fully ready before any network-dependent services start
     */
    fun initializeAndWaitForTor(
        callback: TorBootstrapCallback,
        jobsManager: network.bisq.mobile.domain.utils.CoroutineJobsManager
    ) {
        log.i { "🚀 Bootstrap: Initializing embedded Tor daemon and waiting for ready state..." }

        callback.onTorStateChanged("Initializing Tor daemon...", 0.05f)

        setupTorStateObserver(callback, jobsManager)

        torMonitoringJob = torBootstrapScope.launch {
            try {
                // Start Tor with retry logic
                torIntegrationService.initializeAndStart(
                    maxRetries = 3,
                    retryDelayMs = 5000
                )

                log.i { "✅ Bootstrap: Tor initialization started - waiting for ready state..." }
                callback.onTorStateChanged("Starting Tor daemon...", 0.1f)

                // Add periodic status checks during the wait
                val statusCheckJob = launch {
                    repeat(12) { // Check every 5 seconds for 60 seconds total
                        delay(5000)
                        val currentState = torIntegrationService.torState.value
                        val currentPort = torIntegrationService.socksPort.value
                        log.i { "🔍 Bootstrap: Tor status check - State: $currentState, Port: $currentPort" }

                        // Update UI based on current state
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
                            else -> { /* Keep current state */ }
                        }
                    }
                }

                // Wait for Tor to become ready with timeout
                val torReady = withTimeoutOrNull(60000) { // 60 second timeout
                    torBootstrapComplete.await()
                }

                statusCheckJob.cancel() // Stop status checks

                if (torReady == true) {
                    log.i { "🚀 Bootstrap: Tor is ready - proceeding with application bootstrap" }
                    callback.onTorStateChanged("Tor ready - Starting Bisq...", 0.25f)
                    delay(1000) // Show message briefly
                    
                    // Get the SOCKS port and notify callback
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

    /**
     * Handle Tor timeout with debug and recovery attempts
     */
    private suspend fun handleTorTimeout(callback: TorBootstrapCallback) {
        callback.onTorStateChanged("Tor timeout - Debugging...", 0.2f)
        
        // Debug and try to fix Tor status
        torIntegrationService.debugAndFixTorStatus()

        // Wait a bit for the debug fix to take effect
        delay(2000)

        // Final status check before giving up
        val finalState = torIntegrationService.torState.value
        val finalPort = torIntegrationService.socksPort.value
        log.w { "⚠️ Bootstrap: Final Tor status after debug - State: $finalState, Port: $finalPort" }

        // If Tor is actually ready but we missed the signal, proceed anyway
        if (finalState == TorService.TorState.READY && finalPort != null) {
            log.i { "🚀 Bootstrap: Tor was actually ready after debug - proceeding with bootstrap" }
            callback.onTorStateChanged("Tor ready - Starting Bisq...", 0.25f)
            delay(1000)
            callback.onTorReady(finalPort)
        } else {
            callback.onTorTimeout()
        }
    }

    /**
     * Set up observer for Tor state changes to detect when it becomes ready
     */
    private fun setupTorStateObserver(
        callback: TorBootstrapCallback,
        jobsManager: network.bisq.mobile.domain.utils.CoroutineJobsManager
    ) {
        // Launch a coroutine to collect from the StateFlow
        // Use a separate job that completes once Tor is ready
        jobsManager.addJob(torBootstrapScope.launch {
            try {
                var shouldContinue = true

                // Monitor both Tor state and SOCKS port simultaneously
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
                            // Check if we have both READY state and SOCKS port
                            checkTorReadiness()
                            shouldContinue = false // Stop collecting once we reach READY
                        }
                        TorService.TorState.ERROR -> {
                            log.e { "❌ Bootstrap: Tor encountered an error" }
                            // Complete with failure
                            if (!torBootstrapComplete.isCompleted) {
                                torBootstrapComplete.complete(false)
                            }
                            // Stop collecting on error
                            shouldContinue = false
                        }
                        else -> {
                            // Other states, continue waiting
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

        // Also monitor SOCKS port separately to handle cases where port is available after READY state
        jobsManager.addJob(torBootstrapScope.launch {
            try {
                torIntegrationService.socksPort.collect { socksPort ->
                    log.i { "🔍 Bootstrap: SOCKS port changed to: $socksPort" }

                    // If we have both READY state and SOCKS port, complete bootstrap
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

            // Complete the deferred to signal Tor is ready
            if (!torBootstrapComplete.isCompleted) {
                torBootstrapComplete.complete(true)
            }
        } else {
            log.i { "⏳ Bootstrap: Tor not fully ready yet - waiting for both READY state and SOCKS port" }
        }
    }

    /**
     * Cancel Tor monitoring (called during deactivation)
     */
    fun cancelTorMonitoring(bootstrapSuccessful: Boolean) {
        if (torBootstrapComplete.isCompleted) {
            // Only cancel Tor monitoring if bootstrap was not successful
            // If bootstrap was successful, let Tor continue running
            if (!bootstrapSuccessful) {
                log.w { "⚠️ Bootstrap failed - cancelling Tor monitoring" }
                torMonitoringJob?.cancel()
                torMonitoringJob = null

                // Reset the CompletableDeferred only if bootstrap failed
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
