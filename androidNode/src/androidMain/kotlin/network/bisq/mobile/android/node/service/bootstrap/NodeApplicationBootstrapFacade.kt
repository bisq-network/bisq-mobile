package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.network.TransportType
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.network.KmpTorService
import network.bisq.mobile.android.node.service.network.KmpTorService.State.IDLE
import network.bisq.mobile.android.node.service.network.KmpTorService.State.STARTED
import network.bisq.mobile.android.node.service.network.KmpTorService.State.STARTING
import network.bisq.mobile.android.node.service.network.KmpTorService.State.STARTING_FAILED
import network.bisq.mobile.android.node.service.network.KmpTorService.State.STOPPED
import network.bisq.mobile.android.node.service.network.KmpTorService.State.STOPPING
import network.bisq.mobile.android.node.service.network.KmpTorService.State.STOPPING_FAILED
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import java.io.File

class NodeApplicationBootstrapFacade(
    private val provider: AndroidApplicationService.Provider,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val connectivityService: ConnectivityService,
    private val kmpTorService: KmpTorService,
) : ApplicationBootstrapFacade() {

    companion object {
        private const val DEFAULT_CONNECTIVITY_TIMEOUT_MS = 15000L
        private const val BOOTSTRAP_STAGE_TIMEOUT_MS = 20000L // 20 seconds per stage
    }

    private val applicationServiceState: Observable<State> by lazy { provider.state.get() }
    private var applicationServiceStatePin: Pin? = null
    private var bootstrapSuccessful = false
    private var currentTimeoutJob: Job? = null
    private var torWasStartedBefore = false

    override fun activate() {
        log.i { "Bootstrap: activate() called - isActive: $isActive" }

        // TODO not working for the first translation requested, but avoids crash at least using default
        makeSureI18NIsReady(settingsServiceFacade.languageCode.value)

        if (isActive) {
            log.d { "Bootstrap already active, forcing reset" }
            deactivate()
        }

        super.activate()
        log.i { "Bootstrap: super.activate() completed, calling onInitializeAppState()" }

        observeTorState()
        observeApplicationState()

        onInitializeAppState()
    }

    override fun deactivate() {
        log.i { "Bootstrap: deactivate() called" }
        cancelTimeout()
        stopListeningToBootstrapProcess()

        isActive = false
        super.deactivate()
        log.i { "Bootstrap: deactivate() completed" }
    }

    private fun observeTorState() {
        serviceScope.launch {
            kmpTorService.state.collect { newState ->
                when (newState) {
                    IDLE -> {}
                    STARTING -> {
                        setState("bootstrap.initializingTor".i18n())
                        setProgress(0.1f)
                        startTimeoutForStage()
                    }

                    STARTED -> {
                        setState("bootstrap.torReady".i18n())
                        setProgress(0.25f)
                    }

                    STOPPING -> {}
                    STOPPED -> {}
                    STARTING_FAILED -> {
                        val failure = kmpTorService.startupFailure.value
                        val errorMessage = listOfNotNull(
                            failure?.message,
                            failure?.cause?.message
                        ).firstOrNull() ?: "Unknown Tor error"
                        setState("bootstrap.torError".i18n() + ": $errorMessage")
                        setProgress(0f)
                        cancelTimeout(showProgressToast = false) // Don't show progress toast on failure
                        setBootstrapFailed(true)
                        log.e { "Bootstrap: Tor initialization failed - $errorMessage" }
                    }

                    STOPPING_FAILED -> {}
                }
            }
        }
    }

    private fun observeApplicationState() {
        log.i { "Bootstrap: Setting up application state observer" }
        applicationServiceStatePin = applicationServiceState.addObserver { state: State ->
            log.i { "Bootstrap: Application state changed to: $state" }
            when (state) {
                State.INITIALIZE_APP -> {
                    onInitializeAppState()
                }

                State.INITIALIZE_NETWORK -> {
                    setState("splash.applicationServiceState.INITIALIZE_NETWORK".i18n())
                    setProgress(0.5f)
                    startTimeoutForStage()
                }


                State.INITIALIZE_WALLET -> {
                }

                State.INITIALIZE_SERVICES -> {
                    setState("splash.applicationServiceState.INITIALIZE_SERVICES".i18n())
                    setProgress(0.75f)
                    startTimeoutForStage()
                }

                State.APP_INITIALIZED -> {
                    isActive = true
                    log.i { "Bootstrap: Application services initialized successfully" }
                    val isConnected = connectivityService.isConnected()
                    log.i { "Bootstrap: Connectivity check - Connected: $isConnected" }

                    if (isConnected) {
                        log.i { "Bootstrap: All systems ready - completing initialization" }
                        onInitialized()
                    } else {
                        log.w { "Bootstrap: No connectivity detected - waiting for connection" }
                        setState("bootstrap.noConnectivity".i18n())
                        setProgress(0.95f)
                        startTimeoutForStage()

                        val connectivityJob = connectivityService.runWhenConnected {
                            log.i { "Bootstrap: Connectivity restored, completing initialization" }
                            onInitialized()
                        }


                        serviceScope.launch {
                            delay(DEFAULT_CONNECTIVITY_TIMEOUT_MS)
                            if (!isActive) {
                                log.w { "Bootstrap: Connectivity timeout - proceeding anyway" }
                                connectivityJob.cancel()
                                onInitialized()
                            }
                        }
                    }
                }

                State.FAILED -> {
                    val errorMessage = getDetailedErrorMessage()
                    setState(errorMessage)
                    setProgress(0f)
                    cancelTimeout(showProgressToast = false) // Don't show progress toast on failure
                    setBootstrapFailed(true)
                    log.e { "Bootstrap: Application service failed - $errorMessage" }
                }
            }
        }
    }

    private fun onInitialized() {
        setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
        setProgress(1f)
        bootstrapSuccessful = true
        cancelTimeout()
        log.i { "Bootstrap completed successfully - Tor monitoring will continue" }
    }

    private fun onInitializeAppState() {
        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        val progress = if (isTorSupported()) 0.25f else 0f
        setProgress(progress)
        startTimeoutForStage()
    }

    /* private fun initializeTorAndProceed() {
         launchIO {
             try {
                 log.i { "Bootstrap: Starting Tor daemon initialization..." }
                 // This blocks until Tor is ready
                 val baseDir = provider.applicationService.config.baseDir!!
                 kmpTorService.startTor(baseDir).await()
                 log.i { "Bootstrap: Tor daemon initialized successfully" }
                 torWasStartedBefore = true
                 torInitializationCompleted.complete(Unit)
             } catch (e: Exception) {
                 log.e(e) { "Bootstrap: Failed to initialize Tor daemon" }
                 torInitializationCompleted.complete(Unit)
             }
         }
     }*/

    override suspend fun waitForTor() {
        /* if (isTorSupported()) {
             log.i { "Bootstrap: Waiting for Tor initialization to complete..." }
             torInitializationCompleted.await()
             // Wait briefly for Bisq2 to write external_tor.config to avoid false negatives
             checkForTorConfigFile()
             log.i { "Bootstrap: Tor initialization wait completed" }
         } else {
             log.d { "Bootstrap: CLEARNET configuration - no Tor wait required" }
         }*/
    }

    private suspend fun checkForTorConfigFile(
        maxWaitMs: Long = 5_000,
        pollMs: Long = 100
    ) {
        try {
            val baseDir = provider.applicationService.config.baseDir!!
            val configFile = File(File(baseDir.toFile(), "tor"), "external_tor.config")

            withTimeout(maxWaitMs) {
                while (!configFile.exists()) {
                    delay(pollMs)
                }
            }

            // Small grace period for downstream detection
            delay(200)
            log.i { "Bootstrap: Tor configuration verified and ready (${configFile.absolutePath})" }
        } catch (e: TimeoutCancellationException) {
            log.e { "Bootstrap: external_tor.config not detected within ${maxWaitMs}ms" }
            throw RuntimeException("external_tor.config not found within ${maxWaitMs}ms timeout")
        } catch (e: Exception) {
            log.e(e) { "Bootstrap: Error verifying Tor configuration" }
            throw e
        }
    }

    private fun stopListeningToBootstrapProcess() {
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null
    }

    private fun isTorSupported(): Boolean {
        return provider.applicationService.networkServiceConfig!!.supportedTransportTypes.contains(TransportType.TOR)
    }

    private fun validateTorConfiguration(applicationService: AndroidApplicationService) {
        try {
            // Check if we have both CLEAR and TOR configured (which could cause issues)
            val networkServiceConfig = applicationService.networkServiceConfig
            val supportedTransportTypes = networkServiceConfig.supportedTransportTypes
            if (supportedTransportTypes.contains(TransportType.CLEAR) &&
                supportedTransportTypes.contains(TransportType.TOR)
            ) {
                log.w { "Bootstrap: Both CLEAR and TOR transports are configured - this may cause initialization issues" }
            }

            // Check if external Tor is properly configured
            val torConfig = networkServiceConfig.configByTransportType[TransportType.TOR]
            if (torConfig != null) {
                log.i { "Bootstrap: Tor configuration found - assuming external Tor is configured" }
                // Note: We can't easily access the useExternalTor property here, but the config file shows it's set to true
            }
        } catch (e: Exception) {
            log.w(e) { "Bootstrap: Error validating Tor configuration" }
        }
    }

    private fun startTimeoutForStage(stageName: String = state.value, extendedTimeout: Boolean = false) {
        currentTimeoutJob?.cancel()
        setTimeoutDialogVisible(false)
        setCurrentBootstrapStage(stageName)

        val timeoutDuration = if (extendedTimeout) {
            BOOTSTRAP_STAGE_TIMEOUT_MS * 3 // 3x longer for extended wait (~60s)
        } else {
            BOOTSTRAP_STAGE_TIMEOUT_MS //  Normal timeout (~20s)
        }

        log.i { "Bootstrap: Starting timeout for stage: $stageName (${timeoutDuration / 1000}s)" }

        currentTimeoutJob = serviceScope.launch {
            try {
                delay(timeoutDuration)
                if (!(isActive && bootstrapSuccessful)) {
                    log.w { "Bootstrap: Timeout reached for stage: $stageName" }
                    setTimeoutDialogVisible(true)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    log.d { "Bootstrap: Timeout job cancelled for stage: $stageName" }
                } else {
                    log.e(e) { "Bootstrap: Error in Timeout job, cancelled for stage: $stageName" }
                }
            }
        }
    }

    private fun cancelTimeout(showProgressToast: Boolean = true) {
        currentTimeoutJob?.cancel()
        currentTimeoutJob = null

        // If dialog was visible and we're cancelling due to progress, show toast
        if (isTimeoutDialogVisible.value && showProgressToast) {
            setShouldShowProgressToast(true)
        }

        setTimeoutDialogVisible(false)
    }

    override fun extendTimeout() {
        log.i { "Bootstrap: Extending timeout for current stage" }
        val currentStage = currentBootstrapStage.value
        if (currentStage.isNotEmpty()) {
            // Restart timeout with double the duration for extended wait
            startTimeoutForStage(currentStage, extendedTimeout = true)
        }
        setTimeoutDialogVisible(false)
    }

    override suspend fun stopBootstrapForRetry() {
        log.i { "Bootstrap: User requested to stop bootstrap for retry" }
        stopListeningToBootstrapProcess()
        // Cancel any ongoing timeouts without showing progress toast
        cancelTimeout(showProgressToast = false)

        // Kill Tor process if it was started
        if (isTorSupported()) {
            log.i { "Bootstrap: Stopping Tor daemon for retry" }
            try {
                withTimeout(10_000) {
                    kmpTorService.stopTor(true).await()
                }
                torWasStartedBefore = false
                log.i { "Bootstrap: Tor daemon stopped successfully" }
            } catch (e: Exception) {
                log.w(e) { "Bootstrap: Error stopping Tor daemon, continuing with retry" }
            }
        }

        // Purposely fail the bootstrap to show failed state
        setState("bootstrap.retryReady".i18n())
        setProgress(0f)
        setBootstrapFailed(true)
        setTimeoutDialogVisible(false)
        bootstrapSuccessful = false

        log.i { "Bootstrap: Stopped and ready for retry" }
    }

    private fun getDetailedErrorMessage(): String {
        return try {
            // Check if it's a Tor-related error
            val torFailure = kmpTorService.startupFailure.value
            if (torFailure != null) {
                "bootstrap.torError".i18n() + ": " + listOfNotNull(
                    torFailure.message,
                    torFailure.cause?.message
                ).firstOrNull()
            } else {
                // Check if it's a network configuration issue
                val applicationServiceInstance = provider.applicationService
                val networkService = applicationServiceInstance.networkService
                val supportedTransportTypes = networkService.supportedTransportTypes

                if (supportedTransportTypes.contains(TransportType.TOR)) {
                    "bootstrap.networkTorError".i18n()
                } else {
                    "bootstrap.networkError".i18n()
                }
            }
        } catch (e: Exception) {
            log.w(e) { "Bootstrap: Error getting detailed error message" }
            "splash.applicationServiceState.FAILED".i18n()
        }
    }


}