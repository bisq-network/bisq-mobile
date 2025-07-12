package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.network.TransportType
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.network.KmpTorService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.i18n.i18n

class NodeApplicationBootstrapFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val connectivityService: ConnectivityService,
    private val kmpTorService: KmpTorService,
) : ApplicationBootstrapFacade() {

    companion object {
        private const val DEFAULT_CONNECTIVITY_TIMEOUT_MS = 15000L
    }

    private val applicationServiceState: Observable<State> by lazy { applicationService.state.get() }
    private var applicationServiceStatePin: Pin? = null
    private var bootstrapSuccessful = false
    private var torInitializationCompleted = CompletableDeferred<Unit>()
    
    

    override fun activate() {
        if (isActive) {
            log.d { "Bootstrap already active, forcing reset" }
            deactivate()
        }

        super.activate()
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
            setupApplicationStateObserver()
        }
    }

    private fun onInitialized() {
        setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
        setProgress(1f)
        bootstrapSuccessful = true
        log.i { "Bootstrap completed successfully - Tor monitoring will continue" }
    }

    private fun onInitializeAppState() {
        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        val progress = if (isTorSupported()) 0.25f else 0f
        setProgress(progress)
    }

    private fun initializeTorAndProceed() {
        setState("bootstrap.initializingTor".i18n())
        setProgress(0.1f)

        launchIO {
            try {
                log.i { "Bootstrap: Starting Tor daemon initialization..." }

                // This blocks until Tor is ready
                val baseDir = applicationService.applicationService.config.baseDir!!
                kmpTorService.startTor(baseDir).await()
                log.i { "Bootstrap: Tor daemon initialized successfully" }
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
        log.i { "Bootstrap: Tor initialization completed - proceeding with application setup" }
        setupApplicationStateObserver()
    }

    private fun onTorInitializationFail() {
        setState("bootstrap.torFailed".i18n())
        setProgress(0f)
        // Complete Tor initialization even on failure so we can proceed
        torInitializationCompleted.complete(Unit)
        log.w { "Bootstrap: Tor initialization failed - proceeding with application setup anyway" }
        setupApplicationStateObserver()
        // TODO: Handle Tor failure - maybe fallback to clearnet or show error
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
                }


                State.INITIALIZE_WALLET -> {
                }

                State.INITIALIZE_SERVICES -> {
                    setState("splash.applicationServiceState.INITIALIZE_SERVICES".i18n())
                    setProgress(0.75f)
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
        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null
        if (isTorSupported()) {
            kmpTorService.stopTor()
        }

        isActive = false
        super.deactivate()
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

}