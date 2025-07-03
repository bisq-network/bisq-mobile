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
    private var torMonitoringJob: Job? = null
    private var applicationServiceStatePin: Pin? = null
    private var torBootstrapComplete = CompletableDeferred<Boolean>()
    private var bootstrapSuccessful = false
    private var initializationCallback: ApplicationServiceInitializationCallback? = null
    private lateinit var serverSocket: ServerSocket

    private val torBootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track pending onion services to handle SETEVENTS clearing
    private val pendingOnionServices = mutableMapOf<String, Long>()

    fun setInitializationCallback(callback: ApplicationServiceInitializationCallback) {
        this.initializationCallback = callback
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
        log.i { "🚀 Bootstrap: Initializing embedded Tor daemon and waiting for ready state..." }

        // TODO i18n
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
     * Configure Bisq2 for external Tor with a bridge control port
     * This creates a bridge that forwards commands to real kmp-tor control port and handles hidden services
     */
    private fun configureBisqForExternalTorEarly(socksPort: Int) {
        try {
            log.i { "🌉 KMP: Setting up bridge control port for kmp-tor integration" }
            log.i { "   SOCKS proxy: 127.0.0.1:$socksPort" }
            log.i { "   Strategy: Bridge to real kmp-tor control port + hidden service management" }

            // Query the real kmp-tor control port using GETINFO command
            log.i { "🔍 KMP: Querying real kmp-tor control port using GETINFO..." }
            torIntegrationService.queryActualControlPort { realControlPort ->
                if (realControlPort != null) {
                    log.i { "✅ KMP: Real kmp-tor control port detected: $realControlPort" }

                    // Start a bridge control port that forwards to real kmp-tor
                    val bridgeControlPort = startBridgeControlPort(realControlPort)

                    // Set the bridge control port in TorIntegrationService
                    torIntegrationService.setControlPort(bridgeControlPort)

                    // Generate external_tor.config with the bridge control port
                    generateExternalTorConfig(socksPort, bridgeControlPort)

                    // Update SOCKS proxy properties
                    System.setProperty("socksProxyHost", "127.0.0.1")
                    System.setProperty("socksProxyPort", socksPort.toString())
                    System.setProperty("socksProxyVersion", "5")

                    System.setProperty("bisq.torSocksHost", "127.0.0.1")
                    System.setProperty("bisq.torSocksPort", socksPort.toString())

                    log.i { "✅ KMP: Bridge control port and external Tor config created" }
                    log.i { "   Bridge control port: 127.0.0.1:$bridgeControlPort" }
                    log.i { "   Real kmp-tor control port: $realControlPort" }
                    log.i { "   Bisq2 will connect to bridge, which forwards to real kmp-tor" }

                    log.i { "✅ KMP: Bridge control port setup complete - Bridge: $bridgeControlPort -> Real: $realControlPort" }
                } else {
                    log.e { "❌ KMP: Could not detect real kmp-tor control port!" }
                    log.e { "❌ KMP: Bridge setup failed - Bisq2 will not be able to create hidden services" }
                }
            }

        } catch (e: Exception) {
            log.e(e) { "❌ KMP: Failed to configure bridge control port" }
        }
    }

    // Store the real kmp-tor control port for bridge forwarding
    private var realKmpTorControlPort: Int? = null

    /**
     * Read a complete multiline response from Tor control port
     * Tor control protocol uses:
     * - Single line: "250 OK"
     * - Multiline: "250-line1", "250-line2", "250 OK" (final line ends with space, not dash)
     */
    private fun readMultilineResponse(input: java.io.BufferedReader): List<String> {
        val responses = mutableListOf<String>()

        try {
            while (true) {
                val line = input.readLine() ?: break
                responses.add(line)

                // Check if this is the final line of a multiline response
                // Final line starts with "250 " (space), intermediate lines start with "250-" (dash)
                if (line.startsWith("250 ") || (!line.startsWith("250-") && !line.startsWith("250+"))) {
                    // This is the final line, stop reading
                    break
                }
                // Continue reading for multiline responses (250- or 250+)
            }
        } catch (e: Exception) {
            log.e(e) { "❌ KMP: Bridge: Error reading multiline response" }
        }

        return responses
    }

    /**
     * Start asynchronous event listener for real kmp-tor control port
     * This listens for HS_DESC and other events that come asynchronously from Tor
     */
    private fun startEventListener(realControlInput: java.io.BufferedReader, bisqOutput: java.io.BufferedWriter) {
        torBootstrapScope.launch {
            try {
                log.i { "🌉 KMP: Bridge: Starting asynchronous event listener for real kmp-tor events" }

                while (true) {
                    try {
                        // Check if input stream is ready (non-blocking check)
                        if (realControlInput.ready()) {
                            val eventLine = realControlInput.readLine()
                            if (eventLine != null) {
                                // Check if this is an asynchronous event (starts with 6xx)
                                if (eventLine.startsWith("6")) {
                                    log.i { "🌉 KMP: Bridge: ⚡ Received asynchronous event from real kmp-tor: ${eventLine.take(80)}..." }

                                    // Forward the event to Bisq2
                                    bisqOutput.write("$eventLine\r\n")
                                    bisqOutput.flush()

                                    // Special handling for HS_DESC events
                                    if (eventLine.contains("HS_DESC")) {
                                        log.i { "🌉 KMP: Bridge: ✅ Forwarded HS_DESC event to Bisq2: ${eventLine.take(100)}..." }

                                        // Extract onion address for tracking
                                        val parts = eventLine.split(" ")
                                        if (parts.size >= 4) {
                                            val address = parts[3]
                                            if (eventLine.contains("UPLOAD ")) {
                                                log.i { "🌉 KMP: Bridge: 📤 Hidden service UPLOAD event for: $address" }

                                                // Track upload count for this address (for monitoring only)
                                                val uploadCount = pendingOnionServices.getOrDefault(address, 0L) + 1
                                                pendingOnionServices[address] = uploadCount
                                                log.i { "🌉 KMP: Bridge: Upload count for $address: $uploadCount" }

                                            } else if (eventLine.contains("UPLOADED")) {
                                                log.i { "🌉 KMP: Bridge: ✅ Hidden service UPLOADED event for: $address" }
                                                log.i { "🌉 KMP: Bridge: 🎯 REAL UPLOADED confirmation received - hidden service is live!" }
                                                log.i { "🌉 KMP: Bridge: 🎯 This should complete PublishOnionAddressService and continue P2P bootstrap!" }

                                                // Remove from pending after UPLOADED confirmation
                                                pendingOnionServices.remove(address)
                                                log.i { "🌉 KMP: Bridge: Removed $address from pending (UPLOADED confirmed)" }
                                            }
                                        }
                                    } else {
                                        log.d { "🌉 KMP: Bridge: Forwarded other event: ${eventLine.take(50)}..." }
                                    }
                                } else {
                                    log.d { "🌉 KMP: Bridge: Ignoring non-event line: ${eventLine.take(50)}..." }
                                }
                            }
                        }

                        // Small delay to prevent busy waiting
                        delay(50)

                    } catch (e: Exception) {
                        log.w(e) { "⚠️ KMP: Bridge: Error reading event from real control port: ${e.message}" }
                        delay(1000) // Longer delay on error
                    }
                }

            } catch (e: Exception) {
                log.e(e) { "❌ KMP: Bridge: Event listener stopped: ${e.message}" }
            }
        }
    }

    /**
     * Start a bridge control port that forwards commands to real kmp-tor and handles hidden services
     */
    private fun startBridgeControlPort(realControlPort: Int?): Int {
        // Store the real control port for use in bridge handler
        realKmpTorControlPort = realControlPort
        return try {
            serverSocket = ServerSocket(0) // Auto-assign port
            val port = serverSocket.localPort

            log.i { "🌉 KMP: Bridge control port: Starting server on 127.0.0.1:$port" }
            log.i { "🌉 KMP: Bridge will detect and forward to real kmp-tor control port dynamically" }

            // Start a background thread to handle connections
            launchIO {
                try {
                    log.i { "🌉 KMP: Bridge control port: ✅ SERVER READY - listening on 127.0.0.1:$port" }
                    log.i { "🌉 KMP: Bridge control port: Waiting for Bisq2 to connect..." }

                    while (!serverSocket.isClosed) {
                        try {
                            log.d { "🌉 KMP: Bridge control port: Accepting connections on port $port..." }
                            val clientSocket = serverSocket.accept()
                            log.i { "🌉 KMP: Bridge control port: ✅ CLIENT CONNECTED from ${clientSocket.remoteSocketAddress}" }
                            log.i { "🌉 KMP: Bridge control port: This should be Bisq2 connecting to our bridge server" }

                            // Handle client in separate thread
                            handleBridgeControlConnection(clientSocket)
                        } catch (e: java.net.SocketException) {
                            if (!serverSocket.isClosed) {
                                log.w(e) { "🌉 KMP: Bridge control port: Socket exception (server may be closing)" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!serverSocket.isClosed) {
                        log.e(e) { "❌ KMP: Bridge control port: Server error" }
                    } else {
                        log.d { "🌉 KMP: Bridge control port: Server closed normally" }
                    }
                }
            }

            // Give the server a moment to start
            Thread.sleep(100)

            log.i { "🌉 KMP: Bridge control port: Returning port $port to caller" }
            port
        } catch (e: Exception) {
            log.e(e) { "❌ KMP: Failed to start bridge control port" }
            9051 // Fallback port
        }
    }

    /**
     * Handle a connection to our bridge control port
     * This forwards supported commands to real kmp-tor and handles hidden services internally
     */
    private fun handleBridgeControlConnection(socket: java.net.Socket) {
        // Declare real control connection variables outside try block for proper cleanup
        var realControlSocket: java.net.Socket? = null
        var realControlInput: java.io.BufferedReader? = null
        var realControlOutput: java.io.BufferedWriter? = null

        try {
            log.d { "🌉 KMP: Bridge control: Starting connection handler for ${socket.remoteSocketAddress}" }
            socket.soTimeout = 300000 // 5 minute timeout
            socket.keepAlive = true // Enable keep-alive to maintain connection
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream().bufferedWriter()

            // Try to establish connection to real kmp-tor control port for forwarding
            // Use the stored real control port (not the bridge port)
            val realControlPort = realKmpTorControlPort

            if (realControlPort != null) {
                try {
                    log.i { "🌉 KMP: Bridge: Attempting to connect to real kmp-tor control port $realControlPort" }
                    realControlSocket = java.net.Socket("127.0.0.1", realControlPort)
                    realControlInput = realControlSocket.getInputStream().bufferedReader()
                    realControlOutput = realControlSocket.getOutputStream().bufferedWriter()
                    log.i { "🌉 KMP: Bridge: ✅ Connected to real kmp-tor control port $realControlPort" }

                    // Authenticate with real control port
                    log.d { "🌉 KMP: Bridge: Sending AUTHENTICATE to real control port" }
                    realControlOutput.write("AUTHENTICATE\r\n")
                    realControlOutput.flush()
                    val authResponse = realControlInput.readLine()
                    log.i { "🌉 KMP: Bridge: Real control auth response: $authResponse" }

                    if (authResponse?.startsWith("250") == true) {
                        log.i { "🌉 KMP: Bridge: ✅ Successfully authenticated with real kmp-tor control port" }

                        // Start asynchronous event listener for HS_DESC events from real kmp-tor
                        startEventListener(realControlInput, output)
                    } else {
                        log.w { "⚠️ KMP: Bridge: Authentication failed with real control port: $authResponse" }
                        throw Exception("Authentication failed: $authResponse")
                    }

                } catch (e: Exception) {
                    log.e(e) { "❌ KMP: Bridge: FAILED to connect to real control port $realControlPort: ${e.message}" }
                    log.e { "❌ KMP: Bridge: This will cause ADD_ONION commands to fail!" }
                    realControlSocket?.close()
                    realControlSocket = null
                    realControlInput = null
                    realControlOutput = null
                }
            } else {
                log.e { "❌ KMP: Bridge: Real control port is NULL! Cannot forward commands to real kmp-tor" }
                log.e { "❌ KMP: Bridge: realKmpTorControlPort = $realKmpTorControlPort" }
            }

            // Handle commands with bridge logic (no initial greeting - Tor control protocol is command/response)
            while (!socket.isClosed && socket.isConnected) {
                log.d { "🌉 KMP: Bridge control: Waiting for command..." }
                val command = input.readLine()
                if (command == null) {
                    log.d { "🌉 KMP: Bridge control: Client closed connection (readLine returned null)" }
                    break
                }
                log.i { "🌉 KMP: Bridge control received command: '$command'" }

            // Enhanced logging for bootstrap debugging
            if (command.startsWith("ADD_ONION")) {
                log.i { "🌉 KMP: BOOTSTRAP: ADD_ONION command during P2P bootstrap phase" }
            } else if (command.startsWith("SETEVENTS")) {
                log.i { "🌉 KMP: BOOTSTRAP: SETEVENTS command during P2P bootstrap phase" }
            } else if (command.startsWith("GETINFO")) {
                log.i { "🌉 KMP: BOOTSTRAP: GETINFO command during P2P bootstrap phase: ${command.take(50)}" }
            }

                when {
                    command.startsWith("AUTHENTICATE") -> {
                        // Send proper authentication success response
                        output.write("250 OK\r\n")
                        output.flush()
                        log.i { "🌉 KMP: Bridge control: ✅ AUTHENTICATE command successful - sent 250 OK response" }
                    }
                    command.startsWith("GETINFO") -> {
                        // Forward GETINFO commands to real control port if available
                        if (realControlOutput != null && realControlInput != null) {
                            try {
                                log.d { "🌉 KMP: Bridge: Forwarding GETINFO to real control port: $command" }
                                realControlOutput.write("$command\r\n")
                                realControlOutput.flush()

                                // Read complete multiline response from real control port
                                val responses = readMultilineResponse(realControlInput)
                                if (responses.isNotEmpty()) {
                                    // Forward all response lines to Bisq2
                                    responses.forEach { responseLine ->
                                        output.write("$responseLine\r\n")
                                        output.flush()
                                    }
                                    log.d { "🌉 KMP: Bridge: Forwarded ${responses.size} response lines from real control port" }
                                    log.d { "🌉 KMP: Bridge: First line: ${responses.first()}" }
                                    log.d { "🌉 KMP: Bridge: Last line: ${responses.last()}" }
                                } else {
                                    throw Exception("No response from real control port")
                                }
                            } catch (e: Exception) {
                                log.w(e) { "⚠️ KMP: Bridge: Failed to forward GETINFO, using fallback" }
                                // Fallback to local response
                                if (command.startsWith("GETINFO net/listeners/socks")) {
                                    val socksPort = torIntegrationService.socksPort.value ?: 9050
                                    val response = "250 net/listeners/socks=\"127.0.0.1:$socksPort\"\r\n"
                                    output.write(response)
                                    output.flush()
                                    log.d { "🌉 KMP: Bridge: Sent fallback SOCKS response: ${response.trim()}" }
                                } else {
                                    output.write("250 OK\r\n")
                                    output.flush()
                                }
                            }
                        } else {
                            // No real control port available, use fallback
                            if (command.startsWith("GETINFO net/listeners/socks")) {
                                val socksPort = torIntegrationService.socksPort.value ?: 9050
                                val response = "250 net/listeners/socks=\"127.0.0.1:$socksPort\"\r\n"
                                output.write(response)
                                output.flush()
                                log.d { "🌉 KMP: Bridge: Sent fallback SOCKS response: ${response.trim()}" }
                            } else {
                                output.write("250 OK\r\n")
                                output.flush()
                            }
                        }
                    }
                    command.startsWith("SETEVENTS") -> {
                        // CRITICAL: Handle SETEVENTS clearing to prevent premature event listener shutdown
                        if (command.trim() == "SETEVENTS" && pendingOnionServices.isNotEmpty()) {
                            log.i { "🌉 KMP: BOOTSTRAP: ⚠️ CRITICAL: Bisq2 trying to clear SETEVENTS but we have ${pendingOnionServices.size} pending onion services!" }
                            log.i { "🌉 KMP: BOOTSTRAP: 🔧 BLOCKING SETEVENTS clear to keep Bisq2 listening for real UPLOADED events" }
                            log.i { "🌉 KMP: BOOTSTRAP: 🔧 This prevents premature PublishOnionAddressService completion" }

                            // DON'T forward SETEVENTS clear to real kmp-tor - keep receiving real events
                            // But tell Bisq2 it succeeded so it thinks events are cleared
                            output.write("250 OK\r\n")
                            output.flush()

                            pendingOnionServices.keys.forEach { address ->
                                log.i { "🌉 KMP: BOOTSTRAP: 📋 Keeping event listeners active for: $address" }
                            }

                            log.i { "🌉 KMP: BOOTSTRAP: 🔧 Real kmp-tor will continue generating UPLOADED events" }
                            log.i { "🌉 KMP: BOOTSTRAP: 🔧 Bisq2 will receive them and complete PublishOnionAddressService properly" }

                        } else {
                            // Normal SETEVENTS command (registration, not clearing) - forward to real control port
                            if (realControlOutput != null && realControlInput != null) {
                                try {
                                    log.d { "🌉 KMP: Bridge: Forwarding SETEVENTS to real control port: ${command.take(50)}..." }
                                    realControlOutput.write("$command\r\n")
                                    realControlOutput.flush()

                                    // Read complete multiline response from real control port
                                    val responses = readMultilineResponse(realControlInput)
                                    if (responses.isNotEmpty()) {
                                        // Forward all response lines to Bisq2
                                        responses.forEach { responseLine ->
                                            output.write("$responseLine\r\n")
                                            output.flush()
                                        }
                                        log.d { "🌉 KMP: Bridge: Forwarded ${responses.size} SETEVENTS response lines" }
                                    } else {
                                        throw Exception("No response from real control port")
                                    }
                                } catch (e: Exception) {
                                    log.w(e) { "⚠️ KMP: Bridge: Failed to forward SETEVENTS, using fallback" }
                                    output.write("250 OK\r\n")
                                    output.flush()
                                }
                            } else {
                                // No real control port available, use fallback
                                log.d { "🌉 KMP: Bridge: SETEVENTS fallback: ${command.take(50)}..." }
                                if (command.contains("HS_DESC")) {
                                    log.i { "🌉 KMP: Bridge: HS_DESC events registered - ready for onion service operations" }
                                } else if (command.trim() == "SETEVENTS") {
                                    log.i { "🌉 KMP: Bridge: Events cleared" }
                                }
                                output.write("250 OK\r\n")
                                output.flush()
                            }
                        }
                    }
                    command.startsWith("ADD_ONION") -> {
                        // Forward ADD_ONION commands to real control port - NO FALLBACK
                        if (realControlOutput != null && realControlInput != null) {
                            try {
                                log.i { "🌉 KMP: Bridge: Forwarding ADD_ONION to real kmp-tor control port: ${command.take(80)}..." }
                                realControlOutput.write("$command\r\n")
                                realControlOutput.flush()

                                // Read complete multiline response from real control port
                                val responses = readMultilineResponse(realControlInput)
                                if (responses.isNotEmpty()) {
                                    // Forward all response lines to Bisq2
                                    responses.forEach { responseLine ->
                                        output.write("$responseLine\r\n")
                                        output.flush()
                                    }
                                    log.i { "🌉 KMP: Bridge: ✅ Real kmp-tor ADD_ONION response (${responses.size} lines)" }
                                    log.i { "🌉 KMP: Bridge: First line: ${responses.first().take(80)}..." }
                                    if (responses.size > 1) {
                                        log.i { "🌉 KMP: Bridge: Last line: ${responses.last()}" }
                                    }
                                } else {
                                    log.e { "❌ KMP: Bridge: No response from real kmp-tor control port for ADD_ONION" }
                                    output.write("550 No response from Tor control port\r\n")
                                    output.flush()
                                }
                            } catch (e: Exception) {
                                log.e(e) { "❌ KMP: Bridge: FAILED to forward ADD_ONION to real kmp-tor: ${e.message}" }
                                output.write("550 Failed to forward ADD_ONION command\r\n")
                                output.flush()
                            }
                        } else {
                            log.e { "❌ KMP: Bridge: CANNOT forward ADD_ONION - no real control port connection!" }
                            log.e { "❌ KMP: Bridge: realControlOutput = $realControlOutput, realControlInput = $realControlInput" }
                            output.write("550 No connection to Tor control port\r\n")
                            output.flush()
                        }
                    }

                    command.startsWith("RESETCONF") -> {
                        // Forward RESETCONF commands to real control port if available
                        if (realControlOutput != null && realControlInput != null) {
                            try {
                                log.d { "🌉 KMP: Bridge: Forwarding RESETCONF to real control port: ${command.take(50)}..." }
                                realControlOutput.write("$command\r\n")
                                realControlOutput.flush()

                                val response = realControlInput.readLine()
                                if (response != null) {
                                    output.write("$response\r\n")
                                    output.flush()
                                    log.d { "🌉 KMP: Bridge: Forwarded real RESETCONF response: $response" }
                                } else {
                                    throw Exception("No response from real control port")
                                }
                            } catch (e: Exception) {
                                log.w(e) { "⚠️ KMP: Bridge: Failed to forward RESETCONF, using fallback" }
                                output.write("250 OK\r\n")
                                output.flush()
                            }
                        } else {
                            // No real control port available, use fallback
                            log.d { "🌉 KMP: Bridge: RESETCONF fallback: ${command.take(50)}..." }
                            output.write("250 OK\r\n")
                            output.flush()
                        }
                    }
                    command.startsWith("SETCONF") -> {
                        // Forward SETCONF commands to real control port if available
                        if (realControlOutput != null && realControlInput != null) {
                            try {
                                log.d { "🌉 KMP: Bridge: Forwarding SETCONF to real control port: ${command.take(50)}..." }
                                realControlOutput.write("$command\r\n")
                                realControlOutput.flush()

                                val response = realControlInput.readLine()
                                if (response != null) {
                                    output.write("$response\r\n")
                                    output.flush()
                                    log.d { "🌉 KMP: Bridge: Forwarded real SETCONF response: $response" }
                                } else {
                                    throw Exception("No response from real control port")
                                }
                            } catch (e: Exception) {
                                log.w(e) { "⚠️ KMP: Bridge: Failed to forward SETCONF, using fallback" }
                                output.write("250 OK\r\n")
                                output.flush()
                            }
                        } else {
                            // No real control port available, use fallback
                            log.d { "🌉 KMP: Bridge: SETCONF fallback: ${command.take(50)}..." }
                            output.write("250 OK\r\n")
                            output.flush()
                        }
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

            log.d { "🌉 KMP: Bridge control: Command loop ended, closing sockets" }

            // Close real control connection
            try {
                realControlSocket?.close()
                log.d { "🌉 KMP: Bridge: Closed real control port connection" }
            } catch (e: Exception) {
                log.w(e) { "⚠️ KMP: Bridge: Error closing real control port connection" }
            }

            // Close client socket
            socket.close()
            log.d { "🌉 KMP: Bridge control: Client disconnected normally" }

        } catch (e: java.net.SocketTimeoutException) {
            log.w { "🌉 KMP: Bridge control: Socket timeout after 5 minutes - this may indicate P2P bootstrap is taking longer than expected" }
            log.w { "🌉 KMP: Bridge control: Consider investigating P2P network connectivity issues" }
            try { realControlSocket?.close() } catch (ignored: Exception) {}
            try { socket.close() } catch (ignored: Exception) {}
        } catch (e: java.net.SocketException) {
            log.w { "🌉 KMP: Bridge control: Socket exception (client likely disconnected): ${e.message}" }
            try { realControlSocket?.close() } catch (ignored: Exception) {}
            try { socket.close() } catch (ignored: Exception) {}
        } catch (e: Exception) {
            log.e(e) { "❌ KMP: Bridge control connection error" }
            try { realControlSocket?.close() } catch (ignored: Exception) {}
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
                appendLine("# Generated with bridge control port for kmp-tor integration")
                appendLine("# Bridge forwards commands to real kmp-tor control port and handles hidden services")
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