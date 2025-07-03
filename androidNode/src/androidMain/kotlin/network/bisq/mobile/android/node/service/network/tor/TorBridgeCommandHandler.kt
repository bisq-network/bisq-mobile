package network.bisq.mobile.android.node.service.network.tor

import network.bisq.mobile.domain.utils.Logging

/**
 * Handles Tor control protocol commands for the bridge
 * Forwards commands to real kmp-tor control port and manages responses
 */
class TorBridgeCommandHandler(
    private val bridge: TorBisqBridge,
    private val realControlInput: java.io.BufferedReader?,
    private val realControlOutput: java.io.BufferedWriter?,
    private val torIntegrationService: TorIntegrationService
) : Logging {

    /**
     * Handle commands from Bisq2 and forward to real kmp-tor control port
     */
    fun handleCommands(
        input: java.io.BufferedReader,
        output: java.io.BufferedWriter,
        socket: java.net.Socket
    ) {
        while (!socket.isClosed && socket.isConnected) {
            log.d { "🌉 KMP: Bridge control: Waiting for command..." }
            val command = input.readLine()
            if (command == null) {
                log.d { "🌉 KMP: Bridge control: Client closed connection (readLine returned null)" }
                break
            }
            log.i { "🌉 KMP: Bridge control received command: '$command'" }

            // Enhanced logging for bootstrap debugging
            logBootstrapCommand(command)

            when {
                command.startsWith("AUTHENTICATE") -> handleAuthenticate(output)
                command.startsWith("GETINFO") -> handleGetInfo(command, output)
                command.startsWith("SETEVENTS") -> handleSetEvents(command, output)
                command.startsWith("ADD_ONION") -> handleAddOnion(command, output)
                command.startsWith("RESETCONF") -> handleResetConf(command, output)
                command.startsWith("SETCONF") -> handleSetConf(command, output)
                command.startsWith("QUIT") -> {
                    output.write("250 closing connection\r\n")
                    output.flush()
                    break
                }
                else -> handleGenericCommand(command, output)
            }
        }
    }

    private fun logBootstrapCommand(command: String) {
        when {
            command.startsWith("ADD_ONION") -> {
                log.i { "🌉 KMP: BOOTSTRAP: ADD_ONION command during P2P bootstrap phase" }
            }
            command.startsWith("SETEVENTS") -> {
                log.i { "🌉 KMP: BOOTSTRAP: SETEVENTS command during P2P bootstrap phase" }
            }
            command.startsWith("GETINFO") -> {
                log.i { "🌉 KMP: BOOTSTRAP: GETINFO command during P2P bootstrap phase: ${command.take(50)}" }
            }
        }
    }

    private fun handleAuthenticate(output: java.io.BufferedWriter) {
        // Send proper authentication success response
        output.write("250 OK\r\n")
        output.flush()
        log.i { "🌉 KMP: Bridge control: ✅ AUTHENTICATE command successful - sent 250 OK response" }
    }

    private fun handleGetInfo(command: String, output: java.io.BufferedWriter) {
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
                handleGetInfoFallback(command, output)
            }
        } else {
            handleGetInfoFallback(command, output)
        }
    }

    private fun handleGetInfoFallback(command: String, output: java.io.BufferedWriter) {
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

    private fun handleSetEvents(command: String, output: java.io.BufferedWriter) {
        // CRITICAL: Handle SETEVENTS clearing to prevent premature event listener shutdown
        if (command.trim() == "SETEVENTS" && bridge.getPendingOnionServicesCount() > 0) {
            log.i { "🌉 KMP: BOOTSTRAP: ⚠️ CRITICAL: Bisq2 trying to clear SETEVENTS but we have ${bridge.getPendingOnionServicesCount()} pending onion services!" }
            log.i { "🌉 KMP: BOOTSTRAP: 🔧 BLOCKING SETEVENTS clear to keep Bisq2 listening for real UPLOADED events" }
            log.i { "🌉 KMP: BOOTSTRAP: 🔧 This prevents premature PublishOnionAddressService completion" }

            // DON'T forward SETEVENTS clear to real kmp-tor - keep receiving real events
            // But tell Bisq2 it succeeded so it thinks events are cleared
            output.write("250 OK\r\n")
            output.flush()

            bridge.getPendingOnionServicesAddresses().forEach { address ->
                log.i { "🌉 KMP: BOOTSTRAP: 📋 Keeping event listeners active for: $address" }
            }

            log.i { "🌉 KMP: BOOTSTRAP: 🔧 Real kmp-tor will continue generating UPLOADED events" }
            log.i { "🌉 KMP: BOOTSTRAP: 🔧 Bisq2 will receive them and complete PublishOnionAddressService properly" }

        } else {
            // Normal SETEVENTS command (registration, not clearing) - forward to real control port
            forwardSetEventsToRealControl(command, output)
        }
    }

    private fun forwardSetEventsToRealControl(command: String, output: java.io.BufferedWriter) {
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
            handleSetEventsFallback(command, output)
        }
    }

    private fun handleSetEventsFallback(command: String, output: java.io.BufferedWriter) {
        log.d { "🌉 KMP: Bridge: SETEVENTS fallback: ${command.take(50)}..." }
        if (command.contains("HS_DESC")) {
            log.i { "🌉 KMP: Bridge: HS_DESC events registered - ready for onion service operations" }
        } else if (command.trim() == "SETEVENTS") {
            log.i { "🌉 KMP: Bridge: Events cleared" }
        }
        output.write("250 OK\r\n")
        output.flush()
    }

    private fun handleAddOnion(command: String, output: java.io.BufferedWriter) {
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

    private fun handleResetConf(command: String, output: java.io.BufferedWriter) {
        forwardSimpleCommand("RESETCONF", command, output)
    }

    private fun handleSetConf(command: String, output: java.io.BufferedWriter) {
        forwardSimpleCommand("SETCONF", command, output)
    }

    private fun forwardSimpleCommand(commandType: String, command: String, output: java.io.BufferedWriter) {
        if (realControlOutput != null && realControlInput != null) {
            try {
                log.d { "🌉 KMP: Bridge: Forwarding $commandType to real control port: ${command.take(50)}..." }
                realControlOutput.write("$command\r\n")
                realControlOutput.flush()

                val response = realControlInput.readLine()
                if (response != null) {
                    output.write("$response\r\n")
                    output.flush()
                    log.d { "🌉 KMP: Bridge: Forwarded real $commandType response: $response" }
                } else {
                    throw Exception("No response from real control port")
                }
            } catch (e: Exception) {
                log.w(e) { "⚠️ KMP: Bridge: Failed to forward $commandType, using fallback" }
                output.write("250 OK\r\n")
                output.flush()
            }
        } else {
            log.d { "🌉 KMP: Bridge: $commandType fallback: ${command.take(50)}..." }
            output.write("250 OK\r\n")
            output.flush()
        }
    }

    private fun handleGenericCommand(command: String, output: java.io.BufferedWriter) {
        log.d { "🎭 Mock control: Generic command received: '${command.take(30)}...'" }
        output.write("250 OK\r\n")
        output.flush()
    }

    /**
     * Read a complete multiline response from Tor control port
     */
    private fun readMultilineResponse(input: java.io.BufferedReader): List<String> {
        val responses = mutableListOf<String>()

        try {
            while (true) {
                val line = input.readLine() ?: break
                responses.add(line)

                // Check if this is the final line of a multiline response
                if (line.startsWith("250 ") || (!line.startsWith("250-") && !line.startsWith("250+"))) {
                    break
                }
            }
        } catch (e: Exception) {
            log.e(e) { "❌ KMP: Bridge: Error reading multiline response" }
        }

        return responses
    }
}
