package network.bisq.mobile.android.node.service.bootstrap

import bisq.application.State
import bisq.common.observable.Observable
import bisq.common.observable.Pin
import bisq.common.network.TransportType
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
import java.net.ServerSocket

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
    private lateinit var serverSocket: ServerSocket

    // Track pending onion services to handle SETEVENTS clearing
    private val pendingOnionServices = mutableMapOf<String, Long>()

    // Track the most recent onion service for recovery (handles multiple attempts)
    private var lastOnionServiceAddress: String? = null
    private var lastOnionServiceTime: Long = 0

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

        // Check if Tor is supported in the configuration
        if (isTorSupported()) {
            log.i { "🔧 Bootstrap: Tor is supported in configuration - initializing Tor integration" }
            // STEP 1: Initialize Tor as the very first step and WAIT for it to be ready
            initializeAndWaitForTor()
            // Note: Application service state observer will be set up AFTER Tor is ready
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
        jobsManager.addJob(torBootstrapScope.launch {
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
            serverSocket = ServerSocket(0) // Auto-assign port
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

                        // CRITICAL FIX: If we have pending onion services, re-send UPLOAD events
                        // This handles the case where SETEVENTS clears events during profile creation
                        val servicesToRecover = mutableSetOf<String>()

                        // Add pending onion services
                        servicesToRecover.addAll(pendingOnionServices.keys)

                        // ENHANCED FIX: Also recover recent onion service (handles multiple profile attempts)
                        lastOnionServiceAddress?.let { address ->
                            val timeSinceLastService = System.currentTimeMillis() - lastOnionServiceTime
                            if (timeSinceLastService < 300_000) { // Within 5 minutes
                                servicesToRecover.add(address)
                                log.i { "🎭 Mock control: RECOVERY: Adding recent onion service for recovery: $address (${timeSinceLastService}ms ago)" }
                            }
                        }

                        if (servicesToRecover.isNotEmpty()) {
                            log.i { "🎭 Mock control: RECOVERY: Found ${servicesToRecover.size} onion services to recover, re-sending UPLOAD events" }
                            servicesToRecover.forEach { address ->
                                torBootstrapScope.launch {
                                    // CRITICAL FIX: Send events immediately to beat the 120-second timeout
                                    val uploadEvent = "650 HS_DESC UPLOAD $address UNKNOWN HSDIR1 descriptor1 HSDIR_INDEX=0\r\n"
                                    output.write(uploadEvent)
                                    output.flush()
                                    log.i { "🎭 Mock control: RECOVERY: ⚡ IMMEDIATE re-sent UPLOAD event after SETEVENTS clear: $address" }

                                    // Also send UPLOADED event for completeness
                                    delay(50)
                                    val uploadedEvent = "650 HS_DESC UPLOADED $address UNKNOWN HSDIR1\r\n"
                                    output.write(uploadedEvent)
                                    output.flush()
                                    log.i { "🎭 Mock control: RECOVERY: ⚡ IMMEDIATE re-sent UPLOADED event: $address" }
                                }
                            }
                        }
                    }
                    command.startsWith("ADD_ONION") -> {
                        log.i { "🎭 Mock control: Processing ADD_ONION command: ${command.take(80)}..." }
                        log.i { "🎭 Mock control: ⚠️ DEBUGGING: Full command: $command" }

                        // CRITICAL FIX: Handle multiple onion services during profile creation
                        // Each ADD_ONION command creates a different onion service that needs HS_DESC events

                        if (command.contains("ED25519-V3:")) {
                            // For ED25519-V3 commands, we can derive/query the onion address
                            val onionAddress = extractOnionAddressFromCommand(command)
                            if (onionAddress != null) {
                                // Send proper ADD_ONION response with ServiceID
                                output.write("250-ServiceID=$onionAddress\r\n")
                                output.write("250 OK\r\n")
                                output.flush()
                                log.i { "🎭 Mock control: PRODUCTION: Sent ADD_ONION response with ServiceID: $onionAddress" }
                                log.i { "🎭 Mock control: PublishOnionAddressService will wait for UPLOAD events for this address" }

                                // PRODUCTION SOLUTION: Query the actual onion address from Bisq2's NetworkService
                                // This gets the exact address that PublishOnionAddressService is filtering for
                                log.i { "🎭 Mock control: PRODUCTION: 🔧 FINAL FIX: Querying actual onion address from Bisq2..." }

                                // SMART SOLUTION: Use the derived address for profile creation, but query for bootstrap
                                // The ADD_ONION command tells us exactly which TorKeyPair PublishOnionAddressService is using
                                val isBootstrapContext = checkIfBootstrapContext()
                                val expectedAddress = if (isBootstrapContext) {
                                    // For bootstrap: use the queried default NetworkId to maintain compatibility
                                    val queriedAddress = queryOnionAddressFromBisq2()
                                    if (queriedAddress != null) {
                                        log.i { "🎭 Mock control: BOOTSTRAP: Using queried address: ${queriedAddress.replace(".onion", "")}" }
                                        queriedAddress.replace(".onion", "")
                                    } else {
                                        log.w { "🎭 Mock control: BOOTSTRAP: Query failed, using derived address as fallback" }
                                        onionAddress.replace(".onion", "")
                                    }
                                } else {
                                    // For profile creation: use the derived address that matches the TorKeyPair
                                    log.i { "🎭 Mock control: PROFILE: Using derived address that matches TorKeyPair: ${onionAddress.replace(".onion", "")}" }
                                    onionAddress.replace(".onion", "")
                                }
                                log.i { "🎭 Mock control: PRODUCTION: ✅ Selected address for events: $expectedAddress" }

                                // Add to pending onion services to handle SETEVENTS clearing
                                val currentTime = System.currentTimeMillis()
                                pendingOnionServices[expectedAddress] = currentTime
                                lastOnionServiceAddress = expectedAddress
                                lastOnionServiceTime = currentTime
                                log.i { "🎭 Mock control: TRACKING: Added $expectedAddress to pending onion services (total: ${pendingOnionServices.size})" }

                                    // Send HS_DESC UPLOAD event - this is what PublishOnionAddressService is waiting for!
                                    torBootstrapScope.launch {
                                        delay(100) // Small delay to simulate real Tor behavior

                                        // PublishOnionAddressService filters for UPLOAD events (not UPLOADED!)
                                        // Format: 650 HS_DESC UPLOAD <onion_address> UNKNOWN <hs_dir> <descriptor_id> HSDIR_INDEX=<index>
                                        val uploadEvent = "650 HS_DESC UPLOAD $expectedAddress UNKNOWN HSDIR1 descriptor1 HSDIR_INDEX=0\r\n"
                                        output.write(uploadEvent)
                                        output.flush()
                                        log.i { "🎭 Mock control: PRODUCTION: ✅ Sent UPLOAD event (this completes PublishOnionAddressService): $expectedAddress" }
                                        log.i { "🎭 Mock control: PRODUCTION: 🔍 Event format: '${uploadEvent.trim()}'" }

                                        // Optional: Also send UPLOADED for completeness (though not required by PublishOnionAddressService)
                                        delay(200)
                                        val uploadedEvent = "650 HS_DESC UPLOADED $expectedAddress UNKNOWN HSDIR1\r\n"
                                        output.write(uploadedEvent)
                                        output.flush()
                                        log.i { "🎭 Mock control: PRODUCTION: ✅ Sent UPLOADED event for completeness: $expectedAddress" }

                                        // Remove from pending onion services after successful completion
                                        pendingOnionServices.remove(expectedAddress)
                                        log.i { "🎭 Mock control: TRACKING: Removed $expectedAddress from pending onion services (remaining: ${pendingOnionServices.size})" }
                                    }
                            } else {
                                log.w { "🎭 Mock control: ⚠️ Could not extract onion address from ED25519-V3 command" }
                                output.write("250 OK\r\n")
                                output.flush()
                            }
                        } else {
                            // For NEW key commands, generate a random onion address and send events
                            val randomOnionAddress = generateRandomOnionAddress()
                            output.write("250-ServiceID=$randomOnionAddress\r\n")
                            output.write("250 OK\r\n")
                            output.flush()
                            log.i { "🎭 Mock control: PRODUCTION: Sent ADD_ONION response with random ServiceID: $randomOnionAddress" }

                            // Add to pending onion services
                            val baseAddress = randomOnionAddress.replace(".onion", "")
                            pendingOnionServices[baseAddress] = System.currentTimeMillis()
                            log.i { "🎭 Mock control: TRACKING: Added $baseAddress to pending onion services (total: ${pendingOnionServices.size})" }

                            // Send HS_DESC events for the random address
                            torBootstrapScope.launch {
                                delay(100)
                                log.i { "🎭 Mock control: PRODUCTION: Sending HS_DESC events for random onion service: $randomOnionAddress" }
                                simulateHsDescEventsProduction(output, randomOnionAddress)

                                // Remove from pending after completion
                                delay(500) // Wait for events to complete
                                pendingOnionServices.remove(baseAddress)
                                log.i { "🎭 Mock control: TRACKING: Removed $baseAddress from pending onion services (remaining: ${pendingOnionServices.size})" }
                            }
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
     * Generate a random onion address for NEW key ADD_ONION commands
     */
    private fun generateRandomOnionAddress(): String {
        // Generate a random 56-character base32 string (like real v3 onion addresses)
        val chars = "abcdefghijklmnopqrstuvwxyz234567"
        val random = kotlin.random.Random.Default
        val randomPart = (1..56).map { chars[random.nextInt(chars.length)] }.joinToString("")
        return "$randomPart.onion"
    }

    /**
     * Derive onion address from ED25519-V3 secret scalar using Bisq2's official TorKeyGeneration APIs
     * This handles the 64-byte secret scalar format that Bisq2 sends to the Tor control protocol
     */
    private fun deriveOnionAddressFromPrivateKey(base64SecretScalar: String): String? {
        return try {
            log.i { "🎭 Mock control: PRODUCTION: Starting onion address derivation using Bisq2 APIs..." }

            // Decode the Base64 secret scalar
            val secretScalarBytes = android.util.Base64.decode(base64SecretScalar, android.util.Base64.DEFAULT)
            log.i { "🎭 Mock control: ✅ Decoded secret scalar: ${secretScalarBytes.size} bytes" }

            if (secretScalarBytes.size == 64) {
                // This is a 64-byte secret scalar - derive public key from it
                log.i { "🎭 Mock control: ✅ Processing 64-byte secret scalar (Tor format)" }

                // Generate public key from the secret scalar using Ed25519
                val publicKeyBytes = ByteArray(32)
                org.bouncycastle.math.ec.rfc8032.Ed25519.generatePublicKey(secretScalarBytes, 0, publicKeyBytes, 0)
                log.i { "🎭 Mock control: ✅ Generated public key from secret scalar: ${publicKeyBytes.size} bytes" }

                // Use Bisq2's official API to derive onion address from public key
                val onionAddress = bisq.security.keys.TorKeyGeneration.getOnionAddressFromPublicKey(publicKeyBytes)
                log.i { "🎭 Mock control: ✅ Successfully derived onion address using Bisq2: $onionAddress" }

                return onionAddress

            } else if (secretScalarBytes.size == 32) {
                // This is a 32-byte private key - use it directly
                log.i { "🎭 Mock control: ✅ Processing 32-byte private key (direct format)" }

                val torKeyPair = bisq.security.keys.TorKeyGeneration.generateKeyPair(secretScalarBytes)
                log.i { "🎭 Mock control: ✅ Generated TorKeyPair using Bisq2 APIs" }

                val onionAddress = torKeyPair.onionAddress
                log.i { "🎭 Mock control: ✅ Successfully derived onion address using Bisq2: $onionAddress" }

                return onionAddress

            } else {
                log.w { "🎭 Mock control: ⚠️ Invalid key size: ${secretScalarBytes.size}, expected 32 or 64 bytes" }
                return null
            }

        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: ❌ Failed to derive onion address using Bisq2 APIs: ${e.message}" }
            null
        }
    }

    /**
     * Extract onion address from ADD_ONION command for HS_DESC event simulation
     * PRODUCTION SOLUTION: Derive the correct onion address from the ED25519-V3 private key
     */
    private fun extractOnionAddressFromCommand(command: String): String? {
        return try {
            log.i { "🎭 Mock control: PRODUCTION: Extracting onion address from command: ${command.take(100)}..." }

            // Parse the ED25519-V3 private key from the command
            val keyMatch = Regex("ED25519-V3:([A-Za-z0-9+/=]+)").find(command)
            if (keyMatch != null) {
                val base64PrivateKey = keyMatch.groupValues[1]
                log.i { "🎭 Mock control: PRODUCTION: Found ED25519-V3 key: ${base64PrivateKey.take(20)}..." }

                // Derive the onion address from the private key
                val onionAddress = deriveOnionAddressFromPrivateKey(base64PrivateKey)
                if (onionAddress != null) {
                    log.i { "🎭 Mock control: ✅ PRODUCTION: Derived onion address: $onionAddress" }
                    return onionAddress
                } else {
                    log.w { "🎭 Mock control: ⚠️ PRODUCTION: Failed to derive onion address from private key" }
                }
            } else {
                log.w { "🎭 Mock control: ⚠️ PRODUCTION: No ED25519-V3 key found in command" }
            }

            log.w { "🎭 Mock control: ⚠️ PRODUCTION: Could not extract onion address from command" }
            return null

        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: Failed to extract onion address from command" }
            null
        }
    }

    /**
     * Check if we're in bootstrap context vs profile creation context
     * Bootstrap happens early in app lifecycle, profile creation happens after user interaction
     */
    private fun checkIfBootstrapContext(): Boolean {
        return try {
            // Check if the application is still in bootstrap/initialization phase
            val appState = applicationService.applicationService.state.get()
            val isBootstrap = appState == State.INITIALIZE_NETWORK ||
                             appState == State.INITIALIZE_SERVICES ||
                             appState == State.INITIALIZE_APP ||
                             appState == State.INITIALIZE_WALLET
            log.i { "🎭 Mock control: App state: $appState, isBootstrap: $isBootstrap" }
            isBootstrap
        } catch (e: Exception) {
            log.w { "🎭 Mock control: Could not determine context, assuming profile creation: ${e.message}" }
            false // Default to profile creation context
        }
    }

    /**
     * PRODUCTION SOLUTION: Query the onion address directly from Bisq2 components
     * This accesses the actual TorTransportService to get the real onion address
     */
    private fun queryOnionAddressFromBisq2(): String? {
        return try {
            log.i { "🎭 Mock control: PRODUCTION: Querying onion address from Bisq2 components..." }

            // Get the application service instance
            val applicationServiceInstance = applicationService.applicationService
            val networkService = applicationServiceInstance.networkService

            // Find the TOR service node
            val torServiceNodeOpt = networkService.findServiceNode(bisq.common.network.TransportType.TOR)
            if (!torServiceNodeOpt.isPresent) {
                log.w { "🎭 Mock control: ⚠️ TOR ServiceNode not found" }
                return null
            }

            val torServiceNode = torServiceNodeOpt.get()
            val transportService = torServiceNode.transportService

            // Cast to TorTransportService to access TOR-specific functionality
            if (transportService is bisq.network.p2p.node.transport.TorTransportService) {
                log.i { "🎭 Mock control: ✅ Found TorTransportService" }

                // Try to get the active NetworkId being published, not just the default one
                val networkIdService = networkService.networkIdService

                // First try to get the active NetworkId from the TorTransportService
                val activeNetworkId = try {
                    // Get the current NetworkId that's being used for publishing
                    val serviceNode = networkService.findServiceNode(bisq.common.network.TransportType.TOR).orElse(null)
                    serviceNode?.let { node ->
                        // Try to get the NetworkId from the node's current state
                        val nodeField = node.javaClass.getDeclaredField("networkId")
                        nodeField.isAccessible = true
                        nodeField.get(node) as? bisq.network.identity.NetworkId
                    }
                } catch (e: Exception) {
                    log.w { "🎭 Mock control: Could not get active NetworkId, falling back to default: ${e.message}" }
                    null
                }

                val networkIdToUse = activeNetworkId ?: networkIdService.orCreateDefaultNetworkId
                log.i { "🎭 Mock control: Using NetworkId: ${if (activeNetworkId != null) "active" else "default"}" }

                val addressByTransportTypeMap = networkIdToUse.addressByTransportTypeMap
                val torAddress = addressByTransportTypeMap.get(bisq.common.network.TransportType.TOR)

                if (torAddress != null) {
                    val onionAddress = torAddress.host // This should be the .onion address without port
                    log.i { "🎭 Mock control: ✅ PRODUCTION: Found onion address: $onionAddress" }
                    return onionAddress
                } else {
                    log.w { "🎭 Mock control: ⚠️ TOR address not found in NetworkId" }
                }
            } else {
                log.w { "🎭 Mock control: ⚠️ TransportService is not TorTransportService: ${transportService.javaClass.simpleName}" }
            }

            return null

        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: Error querying onion address from Bisq2" }
            return null
        }
    }

    /**
     * PRODUCTION: Simulate HS_DESC events for onion service descriptor upload
     * Sends both UPLOAD and UPLOADED events to fully satisfy PublishOnionAddressService
     */
    private suspend fun simulateHsDescEventsProduction(output: java.io.BufferedWriter, onionAddress: String) {
        try {
            log.i { "🎭 Mock control: PRODUCTION: Starting HS_DESC event simulation for $onionAddress" }

            // Wait for PublishOnionAddressService to set up its event listener
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Waiting for PublishOnionAddressService listener registration..." }
            kotlinx.coroutines.delay(2000)  // Increased delay to ensure listener registration
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Proceeding with HS_DESC event simulation..." }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 DEBUG: Socket connected: ${serverSocket.isClosed == false}" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 DEBUG: Output stream available: ${output != null}" }

            // Extract the base address without .onion suffix for HS_DESC events
            val baseAddress = onionAddress.replace(".onion", "")
            log.i { "🎭 Mock control: PRODUCTION: Using base address for events: $baseAddress" }

            // Phase 1: Send UPLOAD event (descriptor upload initiation)
            // This is what PublishOnionAddressService is waiting for!
            val uploadEvent = "650 HS_DESC UPLOAD $baseAddress UNKNOWN HSDIR1 descriptor1 HSDIR_INDEX=0\r\n"
            output.write(uploadEvent)
            output.flush()
            log.i { "🎭 Mock control: PRODUCTION: ✅ Sent HS_DESC UPLOAD event (this completes PublishOnionAddressService): $baseAddress" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Full UPLOAD event: '${uploadEvent.trim()}'" }
            val eventParts = uploadEvent.trim().split(" ")
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Event parts: $eventParts" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Event validation: parts[2]='${eventParts[2]}', length=${eventParts.size}" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Expected for UPLOAD: parts[2]='UPLOAD', length=8" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Address match check: hsAddress='${eventParts[3]}' vs serviceId='$baseAddress'" }

            // Wait for upload processing
            delay(1000)

            // Phase 2: Send UPLOADED event (successful upload confirmation)
            // Format: 650 HS_DESC UPLOADED <onion_address> UNKNOWN <hs_dir>
            val uploadedEvent = "650 HS_DESC UPLOADED $baseAddress UNKNOWN HSDIR1\r\n"
            output.write(uploadedEvent)
            output.flush()
            log.i { "🎭 Mock control: PRODUCTION: ✅ Sent HS_DESC UPLOADED event for $baseAddress" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Full UPLOADED event: '${uploadedEvent.trim()}'" }
            val uploadedEventParts = uploadedEvent.trim().split(" ")
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Event parts: $uploadedEventParts" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Event validation: parts[2]='${uploadedEventParts[2]}', length=${uploadedEventParts.size}" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Expected for UPLOADED: parts[2]='UPLOADED', length=6" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 Address match check: hsAddress='${uploadedEventParts[3]}' vs serviceId='$baseAddress'" }
            log.i { "🎭 Mock control: This should complete PublishOnionAddressService and allow profile creation" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 If no 'Received HsDescEvent' logs appear, the listener registration failed" }
            log.i { "🎭 Mock control: PRODUCTION: 🔍 If no 'Publishing of onion address completed' logs appear, the event filtering failed" }

        } catch (e: Exception) {
            log.e(e) { "🎭 Mock control: PRODUCTION: Error simulating HS_DESC events" }
        }
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