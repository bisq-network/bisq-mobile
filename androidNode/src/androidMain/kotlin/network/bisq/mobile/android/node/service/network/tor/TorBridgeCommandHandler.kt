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

    fun handleCommands(
        input: java.io.BufferedReader,
        output: java.io.BufferedWriter,
        socket: java.net.Socket
    ) {
        while (!socket.isClosed && socket.isConnected) {
            val command = input.readLine()
            if (command == null) {
                break
            }
            log.i { "Bridge control received command: '$command'" }

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
                log.i { "BOOTSTRAP: ADD_ONION command during P2P bootstrap phase" }
            }
            command.startsWith("SETEVENTS") -> {
                log.i { "BOOTSTRAP: SETEVENTS command during P2P bootstrap phase" }
            }
            command.startsWith("GETINFO") -> {
                log.i { "BOOTSTRAP: GETINFO command during P2P bootstrap phase: ${command.take(50)}" }
            }
        }
    }

    private fun handleAuthenticate(output: java.io.BufferedWriter) {
        output.write("250 OK\r\n")
        output.flush()
        log.i { "Bridge control: AUTHENTICATE command successful - sent 250 OK response" }
    }

    private fun handleGetInfo(command: String, output: java.io.BufferedWriter) {
        if (realControlOutput != null && realControlInput != null) {
            try {
                realControlOutput.write("$command\r\n")
                realControlOutput.flush()

                val responses = readMultilineResponse(realControlInput)
                if (responses.isNotEmpty()) {
                    responses.forEach { responseLine ->
                        output.write("$responseLine\r\n")
                        output.flush()
                    }
                } else {
                    throw Exception("No response from real control port")
                }
            } catch (e: Exception) {
                log.w(e) { "Bridge: Failed to forward GETINFO, using fallback" }
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
            log.d { "Bridge: Sent fallback SOCKS response: ${response.trim()}" }
        } else {
            output.write("250 OK\r\n")
            output.flush()
        }
    }

    private fun handleSetEvents(command: String, output: java.io.BufferedWriter) {
        if (command.trim() == "SETEVENTS" && bridge.getPendingOnionServicesCount() > 0) {
            log.i { "BOOTSTRAP: CRITICAL: Bisq2 trying to clear SETEVENTS but we have ${bridge.getPendingOnionServicesCount()} pending onion services!" }
            log.i { "BOOTSTRAP: BLOCKING SETEVENTS clear to keep Bisq2 listening for real UPLOADED events" }
            log.i { "BOOTSTRAP: This prevents premature PublishOnionAddressService completion" }

            output.write("250 OK\r\n")
            output.flush()

            bridge.getPendingOnionServicesAddresses().forEach { address ->
                log.i { "BOOTSTRAP: Keeping event listeners active for: $address" }
            }

            log.i { "BOOTSTRAP: Real kmp-tor will continue generating UPLOADED events" }
            log.i { "BOOTSTRAP: Bisq2 will receive them and complete PublishOnionAddressService properly" }

        } else {
            forwardSetEventsToRealControl(command, output)
        }
    }

    private fun forwardSetEventsToRealControl(command: String, output: java.io.BufferedWriter) {
        if (realControlOutput != null && realControlInput != null) {
            try {

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

                } else {
                    throw Exception("No response from real control port")
                }
            } catch (e: Exception) {
                log.w(e) { "Bridge: Failed to forward SETEVENTS, using fallback" }
                output.write("250 OK\r\n")
                output.flush()
            }
        } else {
            handleSetEventsFallback(command, output)
        }
    }

    private fun handleSetEventsFallback(command: String, output: java.io.BufferedWriter) {
        if (command.contains("HS_DESC")) {
            log.i { "Bridge: HS_DESC events registered - ready for onion service operations" }
        } else if (command.trim() == "SETEVENTS") {
            log.i { "Bridge: Events cleared" }
        }
        output.write("250 OK\r\n")
        output.flush()
    }

    private fun handleAddOnion(command: String, output: java.io.BufferedWriter) {
        if (realControlOutput != null && realControlInput != null) {
            try {
                log.i { "Bridge: Forwarding ADD_ONION to real kmp-tor control port: ${command.take(80)}..." }
                realControlOutput.write("$command\r\n")
                realControlOutput.flush()

                val responses = readMultilineResponse(realControlInput)
                if (responses.isNotEmpty()) {
                    responses.forEach { responseLine ->
                        output.write("$responseLine\r\n")
                        output.flush()
                    }
                    log.i { "Bridge: Real kmp-tor ADD_ONION response (${responses.size} lines)" }
                } else {
                    log.e { "Bridge: No response from real kmp-tor control port for ADD_ONION" }
                    output.write("550 No response from Tor control port\r\n")
                    output.flush()
                }
            } catch (e: Exception) {
                log.e(e) { "Bridge: FAILED to forward ADD_ONION to real kmp-tor: ${e.message}" }
                output.write("550 Failed to forward ADD_ONION command\r\n")
                output.flush()
            }
        } else {
            log.e { "Bridge: CANNOT forward ADD_ONION - no real control port connection!" }
            log.e { "Bridge: realControlOutput = $realControlOutput, realControlInput = $realControlInput" }
            log.e { "Bridge: This failure is likely due to control port detection failure during bridge setup" }
            log.e { "Bridge: Bridge status: ${bridge.getBridgeConfigurationStatus()}" }
            output.write("550 No connection to Tor control port - bridge not properly configured\r\n")
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
                realControlOutput.write("$command\r\n")
                realControlOutput.flush()

                val response = realControlInput.readLine()
                if (response != null) {
                    output.write("$response\r\n")
                    output.flush()
                } else {
                    throw Exception("No response from real control port")
                }
            } catch (e: Exception) {
                log.w(e) { "Bridge: Failed to forward $commandType, using fallback" }
                log.w { "Bridge: Command was: ${command.take(100)}" }
                output.write("250 OK\r\n")
                output.flush()
            }
        } else {
            output.write("250 OK\r\n")
            output.flush()
        }
    }

    private fun handleGenericCommand(command: String, output: java.io.BufferedWriter) {
        log.d { "🎭 Mock control: Generic command received: '${command.take(30)}...'" }
        output.write("250 OK\r\n")
        output.flush()
    }

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
}
