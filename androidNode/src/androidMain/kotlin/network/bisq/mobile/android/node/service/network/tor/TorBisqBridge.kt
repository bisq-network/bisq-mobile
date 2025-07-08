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

    companion object {
        private const val DEFAULT_SOCKET_TIMEOUT = 180000L
    }

    private val torBootstrapScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var serverSocket: ServerSocket

    private var realKmpTorControlPort: Int? = null
    private val pendingOnionServices = mutableMapOf<String, Long>()

    fun getPendingOnionServicesCount(): Int = pendingOnionServices.size

    fun getPendingOnionServicesAddresses(): Set<String> = pendingOnionServices.keys.toSet()

    /**
     * @return true if the bridge can handle hidden service creation, false otherwise
     */
    fun isBridgeProperlyConfigured(): Boolean {
        return realKmpTorControlPort != null
    }

    /**
     * @return a string describing the bridge configuration status
     */
    fun getBridgeConfigurationStatus(): String {
        return when {
            realKmpTorControlPort == null -> "Bridge not configured - control port detection failed"
            serverSocket.isClosed -> "Bridge configured but server socket closed"
            else -> "Bridge properly configured - control port: $realKmpTorControlPort"
        }
    }

    /**
     * @return a comprehensive status summary for debugging bridge
     */
    fun getBridgeStatusSummary(): String {
        return buildString {
            appendLine("=== Tor Bridge Status Summary ===")
            appendLine("Real kmp-tor control port: $realKmpTorControlPort")
            appendLine("Server socket closed: ${serverSocket.isClosed}")
            appendLine("Pending onion services: ${pendingOnionServices.size}")
            if (pendingOnionServices.isNotEmpty()) {
                appendLine("Pending addresses: ${pendingOnionServices.keys.joinToString(", ")}")
            }
            appendLine("Configuration status: ${getBridgeConfigurationStatus()}")
            appendLine("================================")
        }
    }

    fun configureBisqForExternalTor(socksPort: Int) {
        try {
            log.i { "Setting up bridge control port for kmp-tor integration" }
            log.i { "SOCKS proxy: 127.0.0.1:$socksPort" }
            
            val controlPortDeferred = kotlinx.coroutines.CompletableDeferred<Int>()
            
            torIntegrationService.queryActualControlPort { realControlPort ->
                if (realControlPort != null) {
                    log.i { "Real kmp-tor control port detected: $realControlPort" }
                    controlPortDeferred.complete(realControlPort)
                } else {
                    log.e { "Could not detect real kmp-tor control port!" }
                    log.e { "Bridge setup failed - Bisq2 will not be able to create hidden services" }
                    controlPortDeferred.completeExceptionally(
                        IllegalStateException("Failed to detect kmp-tor control port. Hidden service creation will fail.")
                    )
                }
            }

            val realControlPort = try {
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.withTimeout(10000) { // 10 second timeout
                        controlPortDeferred.await()
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw IllegalStateException("Control port detection timed out after 10 seconds", e)
            }

            val bridgeControlPort = startBridgeControlPort(realControlPort)
            torIntegrationService.setControlPort(bridgeControlPort)
            generateExternalTorConfig(socksPort, bridgeControlPort)
            updateSocksProxyProperties(socksPort)

            log.i { "Bridge control port and external Tor config created" }
            log.i { "Bridge control port: 127.0.0.1:$bridgeControlPort" }
            log.i { "Bridge control port setup complete - Bridge: $bridgeControlPort -> Real: $realControlPort" }

        } catch (e: Exception) {
            log.e(e) { "Failed to configure bridge control port" }
            // Re-throw to make the failure explicit to the caller
            throw e
        }
    }

    private fun updateSocksProxyProperties(socksPort: Int) {
        System.setProperty("socksProxyHost", "127.0.0.1")
        System.setProperty("socksProxyPort", socksPort.toString())
        System.setProperty("socksProxyVersion", "5")

        System.setProperty("bisq.torSocksHost", "127.0.0.1")
        System.setProperty("bisq.torSocksPort", socksPort.toString())
    }

    private fun startBridgeControlPort(realControlPort: Int?): Int {
        realKmpTorControlPort = realControlPort
        return try {
            serverSocket = ServerSocket(0)
            val port = serverSocket.localPort

            log.i { "Bridge control port: Starting server on 127.0.0.1:$port" }

            torBootstrapScope.launch {
                try {
                    log.i { "Bridge control port server ready - listening on 127.0.0.1:$port" }

                    while (!serverSocket.isClosed) {
                        try {
                            val clientSocket = serverSocket.accept()
                            log.i { "Bridge control client connected from ${clientSocket.remoteSocketAddress}" }

                            handleBridgeControlConnection(clientSocket)
                        } catch (e: java.net.SocketException) {
                            if (!serverSocket.isClosed) {
                                log.w(e) { "Bridge control port: Socket exception (server may be closing)" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!serverSocket.isClosed) {
                        log.e(e) { "Bridge control port: Server error" }
                    } else {
                        log.d { "Bridge control port: Server closed normally" }
                    }
                }
            }

            Thread.sleep(100)
            port
        } catch (e: Exception) {
            log.e(e) { "Failed to start bridge control port" }
            9051
        }
    }

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

            val torDir = File(bisq2DataDir, "tor")
            if (!torDir.exists()) {
                torDir.mkdirs()
            }
            val torConfigFile = File(torDir, "external_tor.config")
            torConfigFile.writeText(configContent)
            log.i { "✅ Generated external_tor.config at ${torConfigFile.absolutePath}" }

            log.i { "📄 Config file verification:" }
            log.i { "   Main config: ${configFile.exists()} (${configFile.length()} bytes)" }
            log.i { "   Tor config: ${torConfigFile.exists()} (${torConfigFile.length()} bytes)" }
            log.d { "📄 Config content:\n$configContent" }

        } catch (e: Exception) {
            log.e(e) { "❌ Failed to generate external_tor.config" }
        }
    }

    // useful for mock-tor testing
    private fun readMultilineResponse(input: java.io.BufferedReader): List<String> {
        val responses = mutableListOf<String>()

        try {
            while (true) {
                val line = input.readLine() ?: break
                responses.add(line)

                if (line.startsWith("250 ") || (!line.startsWith("250-") && !line.startsWith("250+"))) {
                    break
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Bridge: Error reading multiline response" }
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
                log.i { "Bridge: Starting asynchronous event listener for real kmp-tor events" }

                while (true) {
                    try {
                        if (realControlInput.ready()) {
                            val eventLine = realControlInput.readLine()
                            if (eventLine != null) {
                                if (eventLine.startsWith("6")) {
                                    log.d { "Bridge: Received asynchronous event from real kmp-tor: ${eventLine.take(80)}..." }

                                    bisqOutput.write("$eventLine\r\n")
                                    bisqOutput.flush()

                                    handleHiddenServiceEvents(eventLine)
                                } else {

                                }
                            }
                        }

                        delay(50)

                    } catch (e: Exception) {
                        log.w(e) { "Bridge: Error reading event from real control port: ${e.message}" }
                        delay(1000)
                    }
                }

            } catch (e: Exception) {
                log.e(e) { "Bridge: Event listener stopped: ${e.message}" }
            }
        }
    }

    private fun handleHiddenServiceEvents(eventLine: String) {
        if (eventLine.contains("HS_DESC")) {
            log.i { "Forwarded HS_DESC event to Bisq2: ${eventLine.take(100)}..." }

            val parts = eventLine.split(" ")
            if (parts.size >= 4) {
                val address = parts[3]
                if (eventLine.contains("UPLOAD ")) {
                    log.i { "Hidden service UPLOAD event for: $address" }

                    val uploadCount = pendingOnionServices.getOrDefault(address, 0L) + 1
                    pendingOnionServices[address] = uploadCount

                } else if (eventLine.contains("UPLOADED")) {
                    log.i { "Hidden service UPLOADED event for: $address" }
                    log.i { "REAL UPLOADED confirmation received - hidden service is live!" }

                    pendingOnionServices.remove(address)
                    log.i { "Removed $address from pending (UPLOADED confirmed)" }
                }
            }
        }
    }

    /**
     * Handle a connection to our bridge control port
     * This forwards supported commands to real kmp-tor and handles hidden services internally
     */
    private fun handleBridgeControlConnection(socket: java.net.Socket) {
        var realControlSocket: java.net.Socket? = null
        var realControlInput: java.io.BufferedReader? = null
        var realControlOutput: java.io.BufferedWriter? = null

        try {
            log.d { "Bridge control: Starting connection handler for ${socket.remoteSocketAddress}" }
            socket.soTimeout = DEFAULT_SOCKET_TIMEOUT.toInt()
            socket.keepAlive = true
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream().bufferedWriter()

            val realControlPort = realKmpTorControlPort

            if (realControlPort != null) {
                try {
                    log.i { "Bridge: Attempting to connect to real kmp-tor control port $realControlPort" }
                    realControlSocket = java.net.Socket("127.0.0.1", realControlPort)
                    realControlInput = realControlSocket.getInputStream().bufferedReader()
                    realControlOutput = realControlSocket.getOutputStream().bufferedWriter()
                    log.i { "Bridge: Connected to real kmp-tor control port $realControlPort" }

                    realControlOutput.write("AUTHENTICATE\r\n")
                    realControlOutput.flush()
                    val authResponse = realControlInput.readLine()

                    if (authResponse?.startsWith("250") == true) {
                        log.i { "Bridge: Successfully authenticated with real kmp-tor control port" }
                        startEventListener(realControlInput, output)
                    } else {
                        log.w { "Bridge: Authentication failed with real control port: $authResponse" }
                        throw Exception("Authentication failed: $authResponse")
                    }

                } catch (e: Exception) {
                    log.e(e) { "Bridge: FAILED to connect to real control port $realControlPort: ${e.message}" }
                    log.e { "Bridge: This will cause ADD_ONION commands to fail!" }
                    realControlSocket?.close()
                    realControlSocket = null
                    realControlInput = null
                    realControlOutput = null
                }
            } else {
                log.e { "Bridge: Real control port is NULL! Cannot forward commands to real kmp-tor" }
                log.e { "Bridge: realKmpTorControlPort = $realKmpTorControlPort" }
            }

            val commandHandler = TorBridgeCommandHandler(this, realControlInput, realControlOutput, torIntegrationService)
            commandHandler.handleCommands(input, output, socket)

            try {
                realControlSocket?.close()
            } catch (e: Exception) {
                log.w(e) { "Bridge: Error closing real control port connection" }
            }

            socket.close()

        } catch (e: java.net.SocketTimeoutException) {
            log.w { "Bridge control: Socket timeout after 5 minutes - this may indicate P2P bootstrap is taking longer than expected" }
            try { realControlSocket?.close() } catch (ignored: Exception) {}
            try { socket.close() } catch (ignored: Exception) {}
        } catch (e: java.net.SocketException) {
            log.w { "Bridge control: Socket exception (client likely disconnected): ${e.message}" }
            try { realControlSocket?.close() } catch (ignored: Exception) {}
            try { socket.close() } catch (ignored: Exception) {}
        } catch (e: Exception) {
            log.e(e) { "Bridge control connection error" }
            try { realControlSocket?.close() } catch (ignored: Exception) {}
            try { socket.close() } catch (ignored: Exception) {}
        }
    }
}
