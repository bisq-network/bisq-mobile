package network.bisq.mobile.android.node.service.network.tor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import network.bisq.mobile.domain.utils.Logging
import java.io.File
import java.net.ServerSocket

/**
 * Handles the bridge between kmp-tor and Bisq2 networking
 * Manages control port forwarding, external configuration, and onion service events
 */
class TorBisqBridge(
    private val torIntegrationService: TorIntegrationService
) : Logging {

    private val torBootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var serverSocket: ServerSocket
    
    // Store the real kmp-tor control port for bridge forwarding
    private var realKmpTorControlPort: Int? = null
    
    // Track pending onion services to handle SETEVENTS clearing
    private val pendingOnionServices = mutableMapOf<String, Long>()

    /**
     * Configure Bisq2 for external Tor with a bridge control port
     * This creates a bridge that forwards commands to real kmp-tor control port and handles hidden services
     */
    fun configureBisqForExternalTor(socksPort: Int) {
        try {
            log.i { "Setting up bridge control port for kmp-tor integration" }
            log.i { "SOCKS proxy: 127.0.0.1:$socksPort" }
            torIntegrationService.queryActualControlPort { realControlPort ->
                if (realControlPort != null) {
                    log.i { "Real kmp-tor control port detected: $realControlPort" }

                    // Start a bridge control port that forwards to real kmp-tor
                    val bridgeControlPort = startBridgeControlPort(realControlPort)

                    // Set the bridge control port in TorIntegrationService
                    torIntegrationService.setControlPort(bridgeControlPort)

                    // Generate external_tor.config with the bridge control port
                    generateExternalTorConfig(socksPort, bridgeControlPort)

                    // Update SOCKS proxy properties
                    updateSocksProxyProperties(socksPort)

                    log.i { "Bridge control port and external Tor config created" }
                    log.i { "Bridge control port: 127.0.0.1:$bridgeControlPort" }

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

    /**
     * Update SOCKS proxy system properties for Bisq2 integration
     */
    private fun updateSocksProxyProperties(socksPort: Int) {
        System.setProperty("socksProxyHost", "127.0.0.1")
        System.setProperty("socksProxyPort", socksPort.toString())
        System.setProperty("socksProxyVersion", "5")

        System.setProperty("bisq.torSocksHost", "127.0.0.1")
        System.setProperty("bisq.torSocksPort", socksPort.toString())
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
            torBootstrapScope.launch {
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
                                    handleHiddenServiceEvents(eventLine)
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
     * Handle hidden service events (HS_DESC UPLOAD/UPLOADED)
     */
    private fun handleHiddenServiceEvents(eventLine: String) {
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
    }

    /**
     * Get pending onion services count (for SETEVENTS clearing logic)
     */
    fun getPendingOnionServicesCount(): Int = pendingOnionServices.size

    /**
     * Get pending onion services addresses (for logging)
     */
    fun getPendingOnionServicesAddresses(): Set<String> = pendingOnionServices.keys.toSet()

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
            val commandHandler = TorBridgeCommandHandler(this, realControlInput, realControlOutput, torIntegrationService)
            commandHandler.handleCommands(input, output, socket)

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
}
