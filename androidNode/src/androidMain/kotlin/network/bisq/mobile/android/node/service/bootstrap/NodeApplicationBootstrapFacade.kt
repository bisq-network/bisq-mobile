package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.network.TransportType
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.network.KmpTorService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n

class NodeApplicationBootstrapFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val connectivityService: ConnectivityService,
    private val kmpTorService: KmpTorService,
) : ApplicationBootstrapFacade() {

    companion object {
        private const val DEFAULT_CONNECTIVITY_TIMEOUT_MS = 15000L
        private const val BOOTSTRAP_STAGE_TIMEOUT_MS = 20000L // 20 seconds per stage
    }

    private val applicationServiceState: Observable<State> by lazy { applicationService.state.get() }
    private var applicationServiceStatePin: Pin? = null
    private var bootstrapSuccessful = false
    private var torInitializationCompleted = CompletableDeferred<Unit>()
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

        // Set up application state observer FIRST, before any initialization
        setupApplicationStateObserver()

        onInitializeAppState()

        // Reset Tor initialization state
        torInitializationCompleted = CompletableDeferred()

        // Check if Tor is required based on configuration
        if (isTorSupported()) {
            log.i { "Bootstrap: TOR transport detected in configuration - initializing Tor daemon..." }
            initializeTorAndProceed()
        } else {
            log.i { "Bootstrap: Tor not required in configuration (CLEARNET only) - skipping Tor initialization" }
            // Complete immediately for CLEARNET
            torInitializationCompleted.complete(Unit)
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

    private fun initializeTorAndProceed() {
        setState("bootstrap.initializingTor".i18n())
        setProgress(0.1f)
        startTimeoutForStage()

        launchIO {
            try {
                log.i { "Bootstrap: Starting Tor daemon initialization..." }
                // This blocks until Tor is ready
                val baseDir = applicationService.applicationService.config.baseDir!!
                kmpTorService.startTor(baseDir).await()
                log.i { "Bootstrap: Tor daemon initialized successfully" }
                torWasStartedBefore = true
                onTorInitializationSuccess()
            } catch (e: Exception)  {
                log.e(e) { "Bootstrap: Failed to initialize Tor daemon" }
                onTorInitializationFail()
            }
        }
    }

    private fun onTorInitializationSuccess() {
        setState("bootstrap.torReady".i18n())
        setProgress(0.25f)
        // Complete Tor initialization
        torInitializationCompleted.complete(Unit)
        log.i { "Bootstrap: Tor initialization completed - application state observer already set up" }
    }

    private fun onTorInitializationFail() {
        setState("bootstrap.torFailed".i18n())
        setProgress(0f)
        cancelTimeout(showProgressToast = false) // Don't show progress toast on failure
        setBootstrapFailed(true)
        // Complete Tor initialization even on failure so we can proceed
        torInitializationCompleted.complete(Unit)
        log.w { "Bootstrap: Tor initialization failed - showing retry option to user" }
    }

    private fun setupApplicationStateObserver() {
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
                    setState("splash.applicationServiceState.FAILED".i18n())
                    setProgress(0f)
                    cancelTimeout(showProgressToast = false) // Don't show progress toast on failure
                    setBootstrapFailed(true)
                }
            }
        }
    }

    override suspend fun waitForTor() {
        if (isTorSupported()) {
            log.i { "Bootstrap: Waiting for Tor initialization to complete..." }
            torInitializationCompleted.await()
            log.i { "Bootstrap: Tor initialization wait completed" }
        } else {
            log.d { "Bootstrap: CLEARNET configuration - no Tor wait required" }
        }
    }

    override fun deactivate() {
        log.i { "Bootstrap: deactivate() called" }
        cancelTimeout()
        stopListeningToBootstrapProcess()

        // Only stop Tor if we haven't already stopped it for retry
        if (isTorSupported() && torWasStartedBefore) {
            log.i { "Bootstrap: Stopping Tor daemon during deactivate" }
            kmpTorService.stopTor().invokeOnCompletion { t ->
                if (t != null) log.w(t) { "Bootstrap: Tor stop failed" }
                else log.i { "Bootstrap: Tor stopped" }
            }
        }

        isActive = false
        super.deactivate()
        log.i { "Bootstrap: deactivate() completed" }
    }

    private fun stopListeningToBootstrapProcess() {
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null
    }

    private fun isTorSupported(): Boolean {
        return try {
            val applicationServiceInstance = applicationService.applicationService
            val networkService = applicationServiceInstance.networkService
            val supportedTransportTypes = networkService.supportedTransportTypes
            val torSupported = supportedTransportTypes.contains(TransportType.TOR)
            log.i { "Bootstrap: Checking Tor support in configuration" }
            log.i { "Supported transport types: $supportedTransportTypes" }
            log.i { "Tor supported: $torSupported" }
            torSupported
        } catch (e: Exception) {
            log.w(e) { "Bootstrap: Could not check Tor support, defaulting to true" }
            true
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

        log.i { "Bootstrap: Starting timeout for stage: $stageName (${timeoutDuration/1000}s)" }

        currentTimeoutJob = serviceScope.launch {
            try {
                delay(timeoutDuration)
                if (!(isActive && bootstrapSuccessful)) {
                    log.w { "Bootstrap: Timeout reached for stage: $stageName" }
                    setTimeoutDialogVisible(true)
                }
            } catch (e: Exception) {
                log.e(e) { "Bootstrap: Timeout job cancelled for stage: $stageName" }
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
            log.i { "Bootstrap: Stopping Tor daemon" }
            kmpTorService.stopTor(true).await()
            torWasStartedBefore = false
        }

        // Purposely fail the bootstrap to show failed state
        setState("splash.applicationServiceState.FAILED".i18n())
        setProgress(0f)
        setBootstrapFailed(true)
        setTimeoutDialogVisible(false)
        bootstrapSuccessful = false

        torInitializationCompleted = CompletableDeferred()

        log.i { "Bootstrap: Stopped and ready for retry" }
    }


}