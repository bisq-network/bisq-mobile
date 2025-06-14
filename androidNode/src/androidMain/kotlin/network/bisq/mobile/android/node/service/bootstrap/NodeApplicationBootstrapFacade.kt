package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CancellationException
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.service.tor.TorIntegrationService
import network.bisq.mobile.android.node.service.tor.TorService
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
    private val torIntegrationService: TorIntegrationService
) : ApplicationBootstrapFacade() {

    private val applicationServiceState: Observable<State> by lazy { applicationService.state.get() }
    private val torBootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectivityJob: Job? = null
    private var torMonitoringJob: Job? = null
    private var applicationServiceStatePin: Pin? = null
    private var torBootstrapComplete = CompletableDeferred<Boolean>()
    private var bootstrapSuccessful = false
    private var initializationCallback: ApplicationServiceInitializationCallback? = null

    fun setInitializationCallback(callback: ApplicationServiceInitializationCallback) {
        this.initializationCallback = callback
    }

    override fun activate() {
        // Check if already active to prevent duplicate activation
        if (isActive) {
            log.d { "Bootstrap already active, forcing reset" }
            // Force reset of bootstrap state to ensure it runs again
            deactivate()
        }

        super.activate()

        // STEP 1: Initialize Tor as the very first step and WAIT for it to be ready
        initializeAndWaitForTor()

        // Note: Application service state observer will be set up AFTER Tor is ready
        // This prevents the bootstrap from proceeding until Tor is fully initialized
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
        log.i { "🚀 Bootstrap: Initializing embedded Tor daemon and waiting for ready state..." }

        // Update bootstrap state to show Tor initialization
        setState("Initializing Tor daemon...")
        setProgress(0.05f) // Very small progress to show we've started

        setupTorStateObserver()

        torMonitoringJob = torBootstrapScope.launch {
            try {
                // Start Tor with retry logic
                torIntegrationService.initializeAndStart(
                    maxRetries = 3,
                    retryDelayMs = 5000
                )

                log.i { "✅ Bootstrap: Tor initialization started - waiting for ready state..." }
                setState("Starting Tor daemon...")
                setProgress(0.1f)

                // Add periodic status checks during the wait
                val statusCheckJob = launch {
                    repeat(12) { // Check every 5 seconds for 60 seconds total
                        delay(5000)
                        setProgress(progress.value + 0.1f)
                        val currentState = torIntegrationService.torState.value
                        val currentPort = torIntegrationService.socksPort.value
                        log.i { "🔍 Bootstrap: Tor status check - State: $currentState, Port: $currentPort" }

                        // Update UI based on current state
                        when (currentState) {
                            TorService.TorState.STARTING -> setState("Starting Tor daemon...")
                            TorService.TorState.BOOTSTRAPPING -> setState("Tor connecting to network...")
                            TorService.TorState.READY -> {
                                if (currentPort != null) {
                                    setState("Tor ready - Starting Bisq...")
                                } else {
                                    setState("Tor almost ready...")
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
                    setState("Tor ready - Starting Bisq...")
                    setProgress(0.25f)
                    delay(1000) // Show message briefly
                    proceedWithApplicationBootstrap()
                } else {
                    log.w { "⚠️ Bootstrap: Tor timeout after 60 seconds" }

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
                        setState("Tor ready - Starting Bisq...")
                        setProgress(0.25f)
                        delay(1000)
                        proceedWithApplicationBootstrap()
                    } else {
                        setState("Tor timeout - Starting Bisq...")
                        setProgress(0.25f)
                        delay(2000) // Show timeout message
                        proceedWithApplicationBootstrap()
                    }
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    log.d { "🔄 Bootstrap: Tor initialization cancelled (normal during deactivation)" }
                    return@launch
                }
                log.e(e) { "❌ Bootstrap: Failed to start Tor initialization" }
                log.w { "⚠️ Bootstrap: Proceeding without Tor - users can enable it in settings" }
                setState("Tor failed - Starting Bisq...")
                setProgress(0.25f)
                delay(2000)
                proceedWithApplicationBootstrap()
            }
        }
    }

    /**
     * Set up observer for Tor state changes to detect when it becomes ready
     */
    private fun setupTorStateObserver() {
        // Launch a coroutine to collect from the StateFlow
        // Use a separate job that completes once Tor is ready
        val stateObserverJob = torBootstrapScope.launch {
            try {
                var shouldContinue = true

                // Monitor both Tor state and SOCKS port simultaneously
                torIntegrationService.torState.collect { torState ->
                    if (!shouldContinue) return@collect

                    log.i { "🔍 Bootstrap: Tor state changed to: $torState" }

                    when (torState) {
                        TorService.TorState.STARTING -> {
                            setState("Starting Tor daemon...")
                            setProgress(0.1f)
                        }
                        TorService.TorState.BOOTSTRAPPING -> {
                            setState("Tor connecting to network...")
                            setProgress(0.15f)
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
        }

        // Also monitor SOCKS port separately to handle cases where port is available after READY state
        val portObserverJob = torBootstrapScope.launch {
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
        }
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
     * Proceed with the normal application bootstrap process
     */
    private fun proceedWithApplicationBootstrap() {
        log.i { "📱 Bootstrap: Starting Bisq application services..." }

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

                        // Set up connectivity monitoring with timeout fallback
                        connectivityJob = connectivityService.runWhenConnected {
                            log.i { "🌐 Bootstrap: Connectivity restored, completing initialization" }
                            onInitialized()
                        }

                        // Add a fallback timeout for connectivity
                        torBootstrapScope.launch {
                            delay(15000) // 15 second timeout for connectivity
                            if (!isActive) { // If bootstrap hasn't completed yet
                                log.w { "⚠️ Bootstrap: Connectivity timeout - proceeding anyway" }
                                connectivityJob?.cancel()
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
        connectivityJob?.cancel()
        connectivityJob = null

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

        applicationServiceStatePin?.unbind()
        applicationServiceStatePin = null

        isActive = false
        super.deactivate()
    }
}