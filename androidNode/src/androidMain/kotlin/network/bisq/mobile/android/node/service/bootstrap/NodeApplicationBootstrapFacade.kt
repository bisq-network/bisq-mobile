package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import bisq.common.network.TransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.network.tor.TorIntegrationService
import network.bisq.mobile.android.node.service.network.tor.TorBootstrapOrchestrator
import network.bisq.mobile.android.node.service.network.tor.TorBisqBridge
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.i18n.i18n

interface ApplicationServiceInitializationCallback {
    fun onApplicationServiceInitialized()
    fun onApplicationServiceInitializationFailed(throwable: Throwable)
}

class NodeApplicationBootstrapFacade(
    private val applicationService: AndroidApplicationService.Provider,
    private val connectivityService: ConnectivityService,
    private val torBootstrapOrchestrator: TorBootstrapOrchestrator,
    private val torBisqBridge: TorBisqBridge
) : ApplicationBootstrapFacade(), TorBootstrapOrchestrator.TorBootstrapCallback {

    private val applicationServiceState: Observable<State> by lazy { applicationService.state.get() }
    private var applicationServiceStatePin: Pin? = null
    private var bootstrapSuccessful = false
    private var initializationCallback: ApplicationServiceInitializationCallback? = null

    private val torBootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setInitializationCallback(callback: ApplicationServiceInitializationCallback) {
        this.initializationCallback = callback
    }

    // TorBootstrapOrchestrator.TorBootstrapCallback implementation
    override fun onTorStateChanged(message: String, progress: Float) {
        setState(message)
        setProgress(progress)
    }

    override fun onTorReady(socksPort: Int) {
        log.i { "🚀 Bootstrap: Tor is ready - proceeding with application bootstrap" }

        // CRITICAL: Configure Bisq for external Tor IMMEDIATELY when Tor becomes ready
        // This must happen BEFORE any Bisq services are initialized
        torBisqBridge.configureBisqForExternalTor(socksPort)

        // Proceed with application bootstrap
        proceedWithApplicationBootstrap()
    }

    override fun onTorTimeout() {
        log.w { "⚠️ Bootstrap: Tor timeout - proceeding with application bootstrap anyway" }
        setState("Tor timeout - Starting Bisq...")
        setProgress(0.25f)
        jobsManager.launchIO {
            delay(2000) // Show timeout message
            proceedWithApplicationBootstrap()
        }
    }

    override fun onTorError(exception: Exception) {
        log.e(exception) { "❌ Bootstrap: Tor initialization failed" }
        log.w { "⚠️ Bootstrap: Proceeding without Tor - users can enable it in settings" }
        setState("Tor failed - Starting Bisq...")
        setProgress(0.25f)
        jobsManager.launchIO {
            delay(2000)
            proceedWithApplicationBootstrap()
        }
    }

    override fun activate() {
        if (isActive) {
            log.d { "Bootstrap already active, forcing reset" }
            deactivate()
        }

        super.activate()

        if (isTorSupported()) {
            log.i { "🔧 Bootstrap: Tor is supported in configuration - initializing Tor integration and waiting.." }
            initializeAndWaitForTor()
        } else {
            log.i { "🔧 Bootstrap: Tor not supported in configuration (CLEARNET only) - skipping Tor initialization" }
            // Skip Tor initialization and proceed directly to application service setup
            onInitializeAppState()
            setupApplicationStateObserver()
            triggerApplicationServiceInitialization()
        }
    }

    private fun onInitialized() {
        setState("splash.applicationServiceState.APP_INITIALIZED".i18n())
        setProgress(1f)
        bootstrapSuccessful = true
        log.i { "🎉 Bootstrap completed successfully - Tor monitoring will continue" }
    }

    private fun onInitializeAppState() {
        setState("splash.applicationServiceState.INITIALIZE_APP".i18n())
        setProgress(0f)
    }

    /**
     * Initialize Tor and wait for it to be ready before proceeding with bootstrap
     * This ensures Tor is fully ready before any network-dependent services start
     */
    private fun initializeAndWaitForTor() {
        log.i { "🚀 Bootstrap: Delegating Tor initialization to TorBootstrapOrchestrator..." }
        torBootstrapOrchestrator.initializeAndWaitForTor(this, jobsManager)
    }

    /**
     * Proceed with the normal application bootstrap process
     */
    private fun proceedWithApplicationBootstrap() {
        log.i { "📱 Bootstrap: Starting Bisq application services..." }

        // Note: SOCKS proxy hijacking was already configured in checkTorReadiness()
        // This ensures system properties are set BEFORE any Bisq services initialize

        // Reset progress and state for application bootstrap
        onInitializeAppState()
        setupApplicationStateObserver()

        // Now that Tor is ready, we can safely initialize the application service
        triggerApplicationServiceInitialization()
    }

    private fun setupApplicationStateObserver() {
        log.i { "📱 Bootstrap: Setting up application state observer..." }
        applicationServiceStatePin = applicationServiceState.addObserver { state: State ->
            log.i { "📱 Bootstrap: Application state changed to: $state" }
            when (state) {
                State.INITIALIZE_APP -> {
                    onInitializeAppState()
                }

                State.INITIALIZE_NETWORK -> {
                    setState("splash.applicationServiceState.INITIALIZE_NETWORK".i18n())
                    setProgress(0.5f)
                }

                // not used
                State.INITIALIZE_WALLET -> {
                }

                State.INITIALIZE_SERVICES -> {
                    setState("splash.applicationServiceState.INITIALIZE_SERVICES".i18n())
                    setProgress(0.75f)
                }

                State.APP_INITIALIZED -> {
                    isActive = true
                    log.i { "✅ Bootstrap: Application services initialized successfully" }

                    // Check connectivity before completing bootstrap
                    val isConnected = connectivityService.isConnected()
                    log.i { "🌐 Bootstrap: Connectivity check - Connected: $isConnected" }

                    if (isConnected) {
                        log.i { "🎉 Bootstrap: All systems ready - completing initialization" }
                        onInitialized()
                    } else {
                        log.w { "⚠️ Bootstrap: No connectivity detected - waiting for connection" }
                        setState("bootstrap.noConnectivity".i18n())
                        setProgress(0.95f) // Not fully complete

                        val connectivityJob = connectivityService.runWhenConnected {
                            log.i { "🌐 Bootstrap: Connectivity restored, completing initialization" }
                            onInitialized()
                        }

                        // Add a fallback timeout for connectivity
                        torBootstrapScope.launch {
                            delay(15000) // 15 second timeout for connectivity
                            if (!isActive) { // If bootstrap hasn't completed yet
                                log.w { "⚠️ Bootstrap: Connectivity timeout - proceeding anyway" }
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

    /**
     * Trigger the actual application service initialization after Tor is ready
     */
    private fun triggerApplicationServiceInitialization() {
        launchIO {
            try {
                log.i { "🚀 Bootstrap: Triggering application service initialization (Tor is ready)..." }

                // Get the application service and check its current state
                val appService = applicationService.applicationService
                val currentState = appService.state.get()

                log.i { "📱 Bootstrap: Current application service state: $currentState" }

                // Check if the service is already initialized
                when (currentState) {
                    State.APP_INITIALIZED -> {
                        log.i { "✅ Bootstrap: Application service already initialized - notifying callback" }
                        initializationCallback?.onApplicationServiceInitialized()
                        return@launchIO
                    }
                    State.FAILED -> {
                        log.w { "⚠️ Bootstrap: Application service is in FAILED state - retrying initialization" }
                    }
                    else -> {
                        log.i { "📱 Bootstrap: Application service in state $currentState - proceeding with initialization" }
                    }
                }

                // Call initialize() which will trigger the state changes we're observing
                appService.initialize()
                    .whenComplete { result: Boolean?, throwable: Throwable? ->
                        if (throwable == null) {
                            if (result == true) {
                                log.i { "✅ Bootstrap: Application service initialization completed successfully" }
                                initializationCallback?.onApplicationServiceInitialized()
                            } else {
                                log.e { "❌ Bootstrap: Application service initialization failed with result=false" }
                                setState("splash.applicationServiceState.FAILED".i18n())
                                setProgress(0f)
                                initializationCallback?.onApplicationServiceInitializationFailed(
                                    RuntimeException("Application service initialization returned false")
                                )
                            }
                        } else {
                            log.e(throwable) { "❌ Bootstrap: Application service initialization failed with exception" }
                            log.e { "❌ Bootstrap: Exception details: ${throwable?.message}" }
                            log.e { "❌ Bootstrap: Exception type: ${throwable?.javaClass?.simpleName}" }
                            throwable?.printStackTrace()
                            setState("splash.applicationServiceState.FAILED".i18n())
                            setProgress(0f)
                            initializationCallback?.onApplicationServiceInitializationFailed(throwable)
                        }
                    }

            } catch (e: Exception) {
                log.e(e) { "❌ Bootstrap: Failed to trigger application service initialization" }
                setState("splash.applicationServiceState.FAILED".i18n())
                setProgress(0f)
                initializationCallback?.onApplicationServiceInitializationFailed(e)
            }
        }
    }

    override fun deactivate() {
        // Delegate Tor monitoring cancellation to the orchestrator
        torBootstrapOrchestrator.cancelTorMonitoring(bootstrapSuccessful)

        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null

        isActive = false
        super.deactivate()
    }

    /**
     * Check if Tor is supported in the network configuration
     */
    private fun isTorSupported(): Boolean {
        return try {
            val applicationServiceInstance = applicationService.applicationService
            val networkService = applicationServiceInstance.networkService
            val supportedTransportTypes = networkService.supportedTransportTypes
            val torSupported = supportedTransportTypes.contains(TransportType.TOR)
            log.i { "🔍 Bootstrap: Checking Tor support in configuration" }
            log.i { "   Supported transport types: $supportedTransportTypes" }
            log.i { "   Tor supported: $torSupported" }
            torSupported
        } catch (e: Exception) {
            log.w(e) { "⚠️ Bootstrap: Could not check Tor support, defaulting to true" }
            true // Default to true to maintain existing behavior if check fails
        }
    }
}