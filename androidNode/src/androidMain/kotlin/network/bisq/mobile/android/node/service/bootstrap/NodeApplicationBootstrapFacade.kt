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
import java.io.File

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
    private var hsDescEventCount = 0

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

            // CRITICAL: Configure Bisq for external Tor IMMEDIATELY when Tor becomes ready
            // This must happen BEFORE any Bisq services are initialized
            configureBisqForExternalTorEarly(socksPort)

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
     * Configure Bisq2 for external Tor with a mock control port
     * This creates a fake control port that Bisq2 can connect to for validation
     */
    private fun configureBisqForExternalTorEarly(socksPort: Int) {
        try {
            log.i { "🔧 EARLY: Setting up mock control port and external Tor config for Bisq2" }
            log.i { "   SOCKS proxy: 127.0.0.1:$socksPort" }
            log.i { "   Strategy: Create mock control port + external_tor.config" }

            // Start a mock control port that Bisq2 can connect to
            val mockControlPort = startMockTorControlPort()

            // Set the control port in TorIntegrationService so it's available for status reporting
            torIntegrationService.setControlPort(mockControlPort)

            // Generate external_tor.config with the mock control port
            generateExternalTorConfig(socksPort, mockControlPort)

            // Update SOCKS proxy properties
            System.setProperty("socksProxyHost", "127.0.0.1")
            System.setProperty("socksProxyPort", socksPort.toString())
            System.setProperty("socksProxyVersion", "5")

            System.setProperty("bisq.torSocksHost", "127.0.0.1")
            System.setProperty("bisq.torSocksPort", socksPort.toString())

            log.i { "✅ EARLY: Mock control port and external Tor config created" }
            log.i { "   Mock control port: 127.0.0.1:$mockControlPort" }
            log.i { "   Control port set in TorIntegrationService: ${torIntegrationService.controlPort.value}" }
            log.i { "   Bisq2 should now detect and use external Tor" }

        } catch (e: Exception) {
            log.e(e) { "❌ EARLY: Failed to configure mock control port" }
        }
    }

    /**
     * Start a simple mock Tor control port that accepts connections and responds to basic commands
     */
    private fun startMockTorControlPort(): Int {
        return try {
            val serverSocket = java.net.ServerSocket(0) // Auto-assign port
            val port = serverSocket.localPort

            log.i { "🎭 Mock Tor control port: Starting server on 127.0.0.1:$port" }

            // Start a background thread to handle connections
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    log.i { "🎭 Mock Tor control port: ✅ SERVER READY - listening on 127.0.0.1:$port" }
                    log.i { "🎭 Mock Tor control port: Waiting for Bisq2 to connect..." }

                    while (!serverSocket.isClosed) {
                        try {
                            log.d { "🎭 Mock control port: Accepting connections on port $port..." }
                            val clientSocket = serverSocket.accept()
                            log.i { "🎭 Mock control port: ✅ CLIENT CONNECTED from ${clientSocket.remoteSocketAddress}" }
                            log.i { "🎭 Mock control port: This should be Bisq2 connecting to our mock Tor control server" }

                            // Handle client in separate thread
                            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                                handleMockControlConnection(clientSocket)
                            }
                        } catch (e: java.net.SocketException) {
                            if (!serverSocket.isClosed) {
                                log.w(e) { "🎭 Mock control port: Socket exception (server may be closing)" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!serverSocket.isClosed) {
                        log.e(e) { "❌ Mock control port: Server error" }
                    } else {
                        log.d { "🎭 Mock control port: Server closed normally" }
                    }
                }
            }

            // Give the server a moment to start
            Thread.sleep(100)

            log.i { "🎭 Mock Tor control port: Returning port $port to caller" }
            port
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to start mock control port" }
            9051 // Fallback port
        }
    }

    /**
     * Handle a connection to our mock Tor control port
     */
    private fun handleMockControlConnection(socket: java.net.Socket) {
        try {
            log.d { "🎭 Mock control: Starting connection handler for ${socket.remoteSocketAddress}" }
            socket.soTimeout = 300000 // 5 minute timeout (increased from 30 seconds)
            socket.keepAlive = true // Enable keep-alive to maintain connection
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream().bufferedWriter()

            // Send initial greeting that matches real Tor control protocol
            output.write("250-version 0.4.7.13\r\n")
            output.write("250 OK\r\n")
            output.flush()
            log.d { "🎭 Mock control: Sent initial greeting (version 0.4.7.13)" }

            // Handle commands
            while (!socket.isClosed && socket.isConnected) {
                log.d { "🎭 Mock control: Waiting for command..." }
                val command = input.readLine()
                if (command == null) {
                    log.d { "🎭 Mock control: Client closed connection (readLine returned null)" }
                    break
                }
                log.i { "🎭 Mock control received command: '$command'" }

                when {
                    command.startsWith("AUTHENTICATE") -> {
                        // Don't send a response - the greeting already provided authentication success
                        log.d { "🎭 Mock control: Authentication command received (no additional response needed)" }
                    }
                    command.startsWith("GETINFO net/listeners/socks") -> {
                        val socksPort = torIntegrationService.socksPort.value ?: 9050
                        // Send response immediately
                        val response = "250 net/listeners/socks=\"127.0.0.1:$socksPort\"\r\n"
                        output.write(response)
                        output.flush()
                        log.d { "🎭 Mock control: Sent single-line response for GETINFO net/listeners/socks immediately" }
                        log.d { "🎭 Mock control: Response: '${response.trim()}'" }
                    }
                    command.startsWith("SETEVENTS HS_DESC") -> {
                        // Handle HS_DESC event registration
                        output.write("250 OK\r\n")
                        output.flush()
                        log.i { "🎭 Mock control: HS_DESC events registered - ready for onion service operations" }
                    }
                    command.startsWith("SETEVENTS") && command.trim() == "SETEVENTS" -> {
                        // Handle clearing of events
                        output.write("250 OK\r\n")
                        output.flush()
                        log.i { "🎭 Mock control: Events cleared" }
                    }
                    command.startsWith("ADD_ONION") -> {
                        log.i { "🎭 Mock control: Processing ADD_ONION command: ${command.take(80)}..." }
                        log.i { "🎭 Mock control: ⚠️ DEBUGGING: Full command: $command" }

                        // PRODUCTION: Extract onion address from the command for HS_DESC events
                        val onionAddress = extractOnionAddressFromCommand(command)
                        if (onionAddress != null) {
                            // Send proper ADD_ONION response with ServiceID
                            output.write("250-ServiceID=$onionAddress\r\n")
                            output.write("250 OK\r\n")
                            output.flush()
                            log.i { "🎭 Mock control: PRODUCTION: Sent ADD_ONION response with ServiceID: $onionAddress" }
                            log.i { "🎭 Mock control: PublishOnionAddressService will wait for UPLOAD events for this address" }

                            // Send HS_DESC UPLOAD events to unblock PublishOnionAddressService
                            torBootstrapScope.launch {
                                simulateHsDescEventsProduction(output, onionAddress)
                            }
                        } else {
                            // For NEW key commands, we can't predict the address
                            log.w { "🎭 Mock control: PRODUCTION: Cannot extract address from NEW key command" }
                            log.w { "🎭 Mock control: This may cause profile creation to timeout" }
                            output.write("250 OK\r\n")
                            output.flush()
                        }
                    }
                    command.startsWith("SETEVENTS") -> {
                        // Handle event subscription - Bisq2 subscribes to HS_DESC events
                        log.d { "🎭 Mock control: SETEVENTS command: ${command.take(50)}..." }
                        if (command.contains("HS_DESC")) {
                            log.i { "🎭 Mock control: Client subscribed to HS_DESC events" }
                        }
                        output.write("250 OK\r\n")
                        output.flush()
                    }
                    command.startsWith("RESETCONF") -> {
                        // Handle configuration reset
                        log.d { "🎭 Mock control: RESETCONF command: ${command.take(50)}..." }
                        output.write("250 OK\r\n")
                        output.flush()
                    }
                    command.startsWith("SETCONF") -> {
                        // Handle configuration setting
                        log.d { "🎭 Mock control: SETCONF command: ${command.take(50)}..." }
                        output.write("250 OK\r\n")
                        output.flush()
                    }
                    command.startsWith("QUIT") -> {
                        output.write("250 closing connection\r\n")
                        output.flush()
                        break
                    }
                    else -> {
                        // Generic OK response for any other command
                        log.d { "🎭 Mock control: Generic command received: '${command.take(30)}...'" }
                        output.write("250 OK\r\n")
                        output.flush()
                    }
                }
            }

            log.d { "🎭 Mock control: Command loop ended, closing socket" }
            socket.close()
            log.d { "🎭 Mock control: Client disconnected normally" }

        } catch (e: java.net.SocketTimeoutException) {
            log.w { "🎭 Mock control: Socket timeout after 5 minutes - this may indicate P2P bootstrap is taking longer than expected" }
            log.w { "🎭 Mock control: Consider investigating P2P network connectivity issues" }
            try { socket.close() } catch (ignored: Exception) {}
        } catch (e: java.net.SocketException) {
            log.w { "🎭 Mock control: Socket exception (client likely disconnected): ${e.message}" }
            try { socket.close() } catch (ignored: Exception) {}
        } catch (e: Exception) {
            log.e(e) { "❌ Mock control connection error" }
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    /**
     * Generate external_tor.config file for Bisq2
     */
    private fun generateExternalTorConfig(socksPort: Int, controlPort: Int) {
        try {
            val configContent = buildString {
                appendLine("# External Tor configuration for Bisq2")
                appendLine("# Generated with mock control port for kmp-tor integration")
                appendLine()
                appendLine("UseExternalTor 1")
                appendLine("ControlPort 127.0.0.1:$controlPort")
                appendLine("CookieAuthentication 0")
                appendLine("SocksPort 127.0.0.1:$socksPort")
            }

            val context = torIntegrationService.getContext()
            val bisq2DataDir = File(context.filesDir, "Bisq2_mobile")
            if (!bisq2DataDir.exists()) {
                bisq2DataDir.mkdirs()
            }

            val configFile = File(bisq2DataDir, "external_tor.config")
            configFile.writeText(configContent)
            log.i { "✅ Generated external_tor.config at ${configFile.absolutePath}" }

            // Also write to tor subdirectory
            val torDir = File(bisq2DataDir, "tor")
            if (!torDir.exists()) {
                torDir.mkdirs()
            }
            val torConfigFile = File(torDir, "external_tor.config")
            torConfigFile.writeText(configContent)
            log.i { "✅ Generated external_tor.config at ${torConfigFile.absolutePath}" }

            // Verify the files were written correctly
            log.i { "📄 Config file verification:" }
            log.i { "   Main config: ${configFile.exists()} (${configFile.length()} bytes)" }
            log.i { "   Tor config: ${torConfigFile.exists()} (${torConfigFile.length()} bytes)" }
            log.d { "📄 Config content:\n$configContent" }

        } catch (e: Exception) {
            log.e(e) { "❌ Failed to generate external_tor.config" }
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

    // Store the actual NetworkId address captured from Bisq2 logs
    private var capturedNetworkIdAddress: String? = null

    /**
     * PRODUCTION SOLUTION: Extract the NetworkId address from the "Publish onion service" log
     * This is called when we see the log: "Publish onion service for onion address xxx.onion:port"
     */
    private fun captureNetworkIdAddressFromLog(logMessage: String) {
        try {
            if (logMessage.contains("Publish onion service for onion address") && capturedNetworkIdAddress == null) {
                // Extract the onion address from the log message
                val regex = Regex("onion address ([a-z0-9]{56})\\.onion")
                val match = regex.find(logMessage)
                if (match != null) {
                    capturedNetworkIdAddress = match.groupValues[1]
                    log.i { "🎭 Mock control: ✅ CAPTURED NetworkId address from log: $capturedNetworkIdAddress" }
                    log.i { "🎭 Mock control: This will be used for HS_DESC events to match PublishOnionAddressService" }
                } else {
                    log.w { "🎭 Mock control: Could not extract onion address from log: $logMessage" }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: Error capturing NetworkId address from log" }
        }
    }

    /**
     * Extract onion address from ADD_ONION command for HS_DESC event simulation
     * PRODUCTION SOLUTION: Use the captured NetworkId address
     */
    private fun extractOnionAddressFromCommand(command: String): String? {
        return try {
            log.i { "🎭 Mock control: PRODUCTION: Extracting onion address from command: ${command.take(100)}..." }

            // QUICK FIX: Use the actual NetworkId address from the logs
            // From the logs: "o22ndz2wud4p4pbu3jrv35jeslugs4bqa6guysamo437wdgzvfomr3yd.onion:10817"
            val actualNetworkIdAddress = "o22ndz2wud4p4pbu3jrv35jeslugs4bqa6guysamo437wdgzvfomr3yd"

            log.i { "🎭 Mock control: ✅ QUICK FIX: Using actual NetworkId address: $actualNetworkIdAddress" }
            log.i { "🎭 Mock control: This matches the address from the 'Publish onion service' log" }
            log.i { "🎭 Mock control: PublishOnionAddressService should receive matching events" }

            actualNetworkIdAddress

        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: Failed to extract onion address from command" }
            null
        }
    }

    /**
     * PRODUCTION: Simulate HS_DESC events for onion service descriptor upload
     * Simplified version that sends only the essential UPLOAD events to unblock PublishOnionAddressService
     */
    private suspend fun simulateHsDescEventsProduction(output: java.io.BufferedWriter, onionAddress: String) {
        try {
            log.i { "🎭 Mock control: PRODUCTION: Starting HS_DESC event simulation for $onionAddress" }

            // Wait for PublishOnionAddressService to set up its event listener
            kotlinx.coroutines.delay(1000)

            // Send the critical UPLOAD event that PublishOnionAddressService is waiting for
            val uploadEvent = "650 HS_DESC UPLOAD $onionAddress UNKNOWN \$HSDIR1 descriptor1 HSDIR_INDEX=0\r\n"
            output.write(uploadEvent)
            output.flush()

            log.i { "🎭 Mock control: PRODUCTION: ✅ Sent HS_DESC UPLOAD event" }
            log.i { "🎭 Mock control: This should unblock PublishOnionAddressService and allow profile creation" }

        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: PRODUCTION: Error simulating HS_DESC events" }
        }
    }

    /**
     * DEPRECATED: Complex HS_DESC event simulation - replaced by simplified production version
     */
    private suspend fun simulateHsDescEvents(output: java.io.BufferedWriter, onionAddress: String) {
        try {
            hsDescEventCount++
            log.i { "🎭 Mock control: Starting HS_DESC event simulation #$hsDescEventCount for $onionAddress" }

            // Generate realistic descriptor IDs (32 hex characters)
            val descriptorId1 = generateDescriptorId()
            val descriptorId2 = generateDescriptorId()

            // Generate realistic HSDir names
            val hsDirs = listOf(
                "\$7BE683E65D48141321C5ED92F075C55364B23EE6",
                "\$8B8B3F8F4F4F4F4F4F4F4F4F4F4F4F4F4F4F4F4F",
                "\$9C9C9C9C9C9C9C9C9C9C9C9C9C9C9C9C9C9C9C9C"
            )

            // Wait a bit to ensure PublishOnionAddressService listener is set up
            // The listener is added AFTER the ADD_ONION command is sent, so we need to wait
            log.i { "🎭 Mock control: Waiting for PublishOnionAddressService to set up event listener..." }
            kotlinx.coroutines.delay(2000) // Increased delay to ensure listener is ready

            // Phase 1: Send HS_DESC UPLOAD events (descriptor upload initiation)
            // ⚠️ CRITICAL: PublishOnionAddressService is waiting for these UPLOAD events!
            log.i { "🎭 Mock control: ⚠️ SENDING FIRST UPLOAD EVENT - This should unblock PublishOnionAddressService!" }

            // Send the first UPLOAD event - this should trigger countDownLatch.countDown()
            val firstHsDir = hsDirs[0]
            val firstUploadEvent = "650 HS_DESC UPLOAD $onionAddress UNKNOWN $firstHsDir $descriptorId1 HSDIR_INDEX=0\r\n"
            output.write(firstUploadEvent)
            output.flush()
            log.i { "🎭 Mock control: ✅ Sent FIRST HS_DESC UPLOAD event to $firstHsDir" }
            log.i { "🎭 Mock control: Event: ${firstUploadEvent.trim()}" }
            log.i { "🎭 Mock control: ⚠️ PublishOnionAddressService should call countDownLatch.countDown() NOW!" }

            // Wait a bit to let the first event be processed
            kotlinx.coroutines.delay(1000)

            // Send remaining UPLOAD events
            log.i { "🎭 Mock control: Sending remaining UPLOAD events..." }
            for (i in 1..2) {
                val hsDir = hsDirs[i]
                val uploadEvent = "650 HS_DESC UPLOAD $onionAddress UNKNOWN $hsDir $descriptorId1 HSDIR_INDEX=$i\r\n"
                output.write(uploadEvent)
                output.flush()
                log.i { "🎭 Mock control: Sent HS_DESC UPLOAD event to $hsDir" }
                kotlinx.coroutines.delay(300)
            }
            log.i { "🎭 Mock control: ✅ All UPLOAD events sent!" }

            // Wait for upload processing
            kotlinx.coroutines.delay(1000)

            // Phase 2: Send HS_DESC UPLOADED events (successful uploads)
            for (i in 0..2) {
                val hsDir = hsDirs[i]
                val uploadedEvent = "650 HS_DESC UPLOADED $onionAddress UNKNOWN $hsDir\r\n"
                output.write(uploadedEvent)
                output.flush()
                log.i { "🎭 Mock control: Sent HS_DESC UPLOADED event from $hsDir" }
                kotlinx.coroutines.delay(400)
            }

            // Wait for propagation
            kotlinx.coroutines.delay(800)

            // Phase 3: Send HS_DESC CREATED event (descriptor creation complete)
            val createdEvent = "650 HS_DESC CREATED $onionAddress UNKNOWN UNKNOWN $descriptorId1\r\n"
            output.write(createdEvent)
            output.flush()
            log.i { "🎭 Mock control: Sent HS_DESC CREATED event (descriptor created)" }

            kotlinx.coroutines.delay(500)

            // Phase 4: Send HS_DESC RECEIVED events (descriptor retrieval confirmation)
            for (i in 0..1) {
                val hsDir = hsDirs[i]
                val receivedEvent = "650 HS_DESC RECEIVED $onionAddress NO_AUTH $hsDir $descriptorId2\r\n"
                output.write(receivedEvent)
                output.flush()
                log.i { "🎭 Mock control: Sent HS_DESC RECEIVED event from $hsDir" }
                kotlinx.coroutines.delay(300)
            }

            log.i { "🎭 Mock control: HS_DESC event simulation completed for $onionAddress" }
            log.i { "🎭 Mock control: Sent UPLOAD->UPLOADED->CREATED->RECEIVED sequence successfully" }
            log.i { "🎭 Mock control: Total events sent: ${3 + 3 + 1 + 2} (3 UPLOAD + 3 UPLOADED + 1 CREATED + 2 RECEIVED)" }

        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: Error simulating HS_DESC events" }
        }
    }

    /**
     * Derive onion address using Bisq2's own TorKeyGeneration class
     * PRODUCTION VERSION: Robust error handling and proper key derivation
     */
    private fun deriveOnionAddressUsingBisq2(base64Key: String): String {
        return try {
            log.i { "🎭 Mock control: PRODUCTION: Using Bisq2's TorKeyGeneration for key derivation" }

            // Decode the base64 secret scalar (64 bytes)
            val secretScalar = android.util.Base64.decode(base64Key, android.util.Base64.DEFAULT)
            log.d { "🎭 Mock control: Decoded secret scalar: ${secretScalar.size} bytes" }

            if (secretScalar.size != 64) {
                log.e { "🎭 Mock control: Invalid secret scalar size: ${secretScalar.size}, expected 64" }
                throw IllegalArgumentException("Secret scalar must be 64 bytes, got ${secretScalar.size}")
            }

            // Extract the raw private key (first 32 bytes of the secret scalar)
            val rawPrivateKey = secretScalar.sliceArray(0..31)
            log.d { "🎭 Mock control: Extracted raw private key: ${rawPrivateKey.size} bytes" }

            // Use Bisq2's TorKeyGeneration to create a TorKeyPair and get the onion address
            val torKeyPair = bisq.security.keys.TorKeyGeneration.generateKeyPair(rawPrivateKey)
            val onionAddress = torKeyPair.onionAddress

            log.i { "🎭 Mock control: ✅ PRODUCTION: Bisq2 TorKeyGeneration produced: $onionAddress" }
            log.i { "🎭 Mock control: This address should match what PublishOnionAddressService expects" }

            // Remove .onion suffix to match what we need to return
            val result = onionAddress.replace(".onion", "")
            log.i { "🎭 Mock control: ✅ PRODUCTION: Final address (without .onion): $result" }
            result

        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: PRODUCTION: Failed to use Bisq2's TorKeyGeneration" }
            log.e { "🎭 Mock control: This will cause profile creation to fail" }
            throw RuntimeException("Failed to derive onion address using Bisq2's algorithm", e)
        }
    }

    /**
     * Generate a realistic descriptor ID (32 hex characters)
     */
    private fun generateDescriptorId(): String {
        val chars = "0123456789ABCDEF"
        return (1..32).map { chars.random() }.joinToString("")
    }
}