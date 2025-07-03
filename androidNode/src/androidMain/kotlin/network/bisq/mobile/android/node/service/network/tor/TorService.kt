package network.bisq.mobile.android.node.service.network.tor

import android.content.Context
import io.matthewnelson.kmp.tor.runtime.Action
import io.matthewnelson.kmp.tor.runtime.RuntimeEvent
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.OnFailure
import io.matthewnelson.kmp.tor.runtime.core.OnSuccess
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import io.matthewnelson.kmp.tor.runtime.core.ctrl.TorCmd
import io.matthewnelson.kmp.tor.runtime.core.TorEvent
import io.matthewnelson.kmp.tor.resource.exec.tor.ResourceLoaderTorExec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.ServiceFacade
import java.io.File

/**
 * Service for managing embedded Tor instance in Bisq Mobile Android Node
 *
 * Uses kmp-tor with Android Native configuration:
 * - resource-exec-tor: Provides Tor executable resources
 * - resource-compilation-exec-tor: Android Native compilation support
 * - resource-compilation-lib-tor: Android Native library compilation support
 */
class TorService(
    private val context: Context,
    private val baseDir: File
) : ServiceFacade() {

    private var torRuntime: TorRuntime? = null
    
    private val _torState = MutableStateFlow(TorState.STOPPED)
    val torState: StateFlow<TorState> = _torState.asStateFlow()
    
    private val _socksPort = MutableStateFlow<Int?>(null)
    val socksPort: StateFlow<Int?> = _socksPort.asStateFlow()
    
    private val _controlPort = MutableStateFlow<Int?>(null)
    val controlPort: StateFlow<Int?> = _controlPort.asStateFlow()

    enum class TorState {
        STOPPED,
        STARTING,
        BOOTSTRAPPING,
        READY,
        STOPPING,
        ERROR
    }

    /**
     * Initialize Tor runtime with configuration
     */
    fun initialize() {
        if (torRuntime != null) {
            log.w { "Tor runtime already initialized" }
            return
        }

        try {
            val workDir = File(baseDir, "tor")
            val cacheDir = File(baseDir, "tor-cache")
            
            // Ensure directories exist
            workDir.mkdirs()
            cacheDir.mkdirs()

            val environment = TorRuntime.Environment.Builder(
                workDirectory = workDir,
                cacheDirectory = cacheDir,
                loader = ResourceLoaderTorExec::getOrCreate
            )

            torRuntime = TorRuntime.Builder(environment) {
                // Observe runtime events
                observerStatic(RuntimeEvent.LISTENERS, OnEvent.Executor.Immediate) { data ->
                    log.d { "Tor Runtime Event: LISTENERS - $data" }
                    handleRuntimeEvent(RuntimeEvent.LISTENERS, data)
                }

                // Observe Tor events
                observerStatic(TorEvent.ERR, OnEvent.Executor.Immediate) { data ->
                    log.e { "🔴 Tor Error: $data" }
                    handleTorEvent(TorEvent.ERR, data)
                }

                observerStatic(TorEvent.WARN, OnEvent.Executor.Immediate) { data ->
                    log.w { "🟡 Tor Warning: $data" }
                    handleTorEvent(TorEvent.WARN, data)
                }

                observerStatic(TorEvent.NOTICE, OnEvent.Executor.Immediate) { data ->
                    log.i { "🔵 Tor Notice: $data" }
                    handleTorEvent(TorEvent.NOTICE, data)
                }

                observerStatic(TorEvent.STATUS_CLIENT, OnEvent.Executor.Immediate) { data ->
                    log.d { "📊 Tor Status Client: $data" }
                    handleTorEvent(TorEvent.STATUS_CLIENT, data)
                }

                // Observer for network listener events to detect actual ports
                observerStatic(TorEvent.STATUS_SERVER, OnEvent.Executor.Immediate) { data ->
                    log.d { "🌐 Tor Status Server: $data" }
                    handleListenerUpdate(data)
                }

                // Configure Tor options
                config { environment ->
                    // Configure SOCKS port (auto-assign to find available port)
                    TorOption.SocksPort.configure { auto() }

                    // Configure control port (auto-assign)
                    TorOption.ControlPort.configure { auto() }

                    // Disable cookie authentication for easier Bisq2 integration
                    // Bisq2 will try no-auth first, which should work with this setting
                    TorOption.CookieAuthentication.configure(false)

                    // Disable client-only mode to allow hidden services
                    TorOption.ClientOnly.configure(false)

                    // Configure data directory
                    TorOption.DataDirectory.configure(workDir)

                    // Disable some features for mobile optimization
                    TorOption.DisableNetwork.configure(false)
                    TorOption.DormantOnFirstStartup.configure(false)
                }

                // Require error and warning events
                required(TorEvent.ERR)
                required(TorEvent.WARN)
                required(TorEvent.NOTICE)
                required(TorEvent.STATUS_CLIENT)
            }

            log.i { "Tor runtime initialized successfully" }

            // The runtime might start automatically, so let's check if we get events
            launchIO {
                kotlinx.coroutines.delay(2000) // Wait 2 seconds
                log.i { "Checking Tor state after initialization: ${_torState.value}" }
            }
            
        } catch (e: Exception) {
            log.e(e) { "Failed to initialize Tor runtime" }
            _torState.value = TorState.ERROR
        }
    }

    /**
     * Start Tor daemon
     */
    fun startTor() {
        val runtime = torRuntime
        if (runtime == null) {
            log.e { "Tor runtime not initialized" }
            _torState.value = TorState.ERROR
            return
        }

        if (_torState.value != TorState.STOPPED) {
            log.w { "Tor is not in stopped state, current state: ${_torState.value}" }
            return
        }

        _torState.value = TorState.STARTING
        log.i { "Starting Tor daemon..." }

        // Use the proper Action.StartDaemon to start the Tor daemon
        runtime.enqueue(
            Action.StartDaemon,
            OnFailure { error ->
                log.e { "❌ Failed to start Tor daemon: $error" }
                _torState.value = TorState.ERROR
            },
            OnSuccess {
                log.i { "✅ Tor daemon start command executed successfully" }
                // The daemon is starting, we'll get bootstrap events as it progresses
            }
        )

        // Add a timeout to check if we get any events
        launchIO {
            kotlinx.coroutines.delay(30000) // Wait 30 seconds for bootstrap
            if (_torState.value == TorState.STARTING) {
                log.w { "⚠️ No bootstrap events received after 30 seconds. Checking if Tor is actually running..." }
                debugTorStatus()
            }
        }
    }

    /**
     * Stop Tor daemon
     */
    fun stopTor() {
        val runtime = torRuntime
        if (runtime == null) {
            log.w { "Tor runtime not initialized" }
            return
        }

        if (_torState.value == TorState.STOPPED) {
            log.w { "Tor is already stopped" }
            return
        }

        _torState.value = TorState.STOPPING
        log.i { "Stopping Tor daemon..." }

        // Use the proper Action.StopDaemon to stop the Tor daemon
        runtime.enqueue(
            Action.StopDaemon,
            OnFailure { error ->
                log.e { "❌ Failed to stop Tor daemon: $error" }
                // Still mark as stopped since we tried
                _torState.value = TorState.STOPPED
            },
            OnSuccess {
                log.i { "✅ Tor daemon stop command executed successfully" }
                _torState.value = TorState.STOPPED
            }
        )
    }

    /**
     * Restart Tor daemon
     */
    fun restartTor() {
        val runtime = torRuntime
        if (runtime == null) {
            log.e { "Tor runtime not initialized" }
            return
        }

        log.i { "Restarting Tor daemon..." }

        // Use the proper Action.RestartDaemon to restart the Tor daemon
        runtime.enqueue(
            Action.RestartDaemon,
            OnFailure { error ->
                log.e { "❌ Failed to restart Tor daemon: $error" }
                _torState.value = TorState.ERROR
            },
            OnSuccess {
                log.i { "✅ Tor daemon restart command executed successfully" }
                _torState.value = TorState.STARTING
            }
        )
    }

    /**
     * Get new Tor identity (new circuit)
     */
    fun newIdentity() {
        val runtime = torRuntime
        if (runtime == null) {
            log.e { "Tor runtime not initialized" }
            return
        }

        if (_torState.value != TorState.READY) {
            log.w { "Tor is not ready, current state: ${_torState.value}" }
            return
        }

        log.i { "Requesting new Tor identity..." }
        
        // For now, just log the request - actual implementation would send NEWNYM signal
        log.i { "✅ New Tor identity requested (simulated)" }
    }

    /**
     * Handle runtime events from Tor
     */
    private fun handleRuntimeEvent(event: RuntimeEvent<*>, data: Any) {
        when (event) {
            RuntimeEvent.LISTENERS -> {
                // Handle listener updates (SOCKS/Control ports)
                handleListenerUpdate(data.toString())
            }
            else -> {
                // Handle other runtime events
                log.d { "Runtime event: $event - $data" }
            }
        }
    }

    /**
     * Handle Tor events
     */
    private fun handleTorEvent(event: TorEvent, data: String) {
        when (event) {
            TorEvent.STATUS_CLIENT -> {
                handleBootstrapStatus(data)
            }
            TorEvent.ERR -> {
                log.e { "Tor Error: $data" }
                _torState.value = TorState.ERROR
            }
            TorEvent.WARN -> {
                log.w { "Tor Warning: $data" }
            }
            TorEvent.NOTICE -> {
                log.i { "🔵 Tor Notice: $data" }
                if (data.contains("Bootstrapped 100%")) {
                    _torState.value = TorState.READY
                    log.i { "✅ Tor is ready for connections" }

                    // Query the actual SOCKS port from Tor using GETINFO command
                    queryActualSocksPort()
                }
            }
            else -> {
                // Handle other Tor events
            }
        }
    }

    /**
     * Handle bootstrap status updates
     */
    private fun handleBootstrapStatus(data: String) {
        if (data.contains("BOOTSTRAP")) {
            // Only set to BOOTSTRAPPING if we're not already READY
            // This prevents overriding the READY state with late bootstrap events
            if (_torState.value != TorState.READY) {
                _torState.value = TorState.BOOTSTRAPPING
                log.d { "Tor bootstrapping: $data" }
            } else {
                log.d { "Tor bootstrap event received but already READY: $data" }
            }
        }
    }

    /**
     * Handle listener updates to extract SOCKS and Control ports
     */
    private fun handleListenerUpdate(data: String) {
        try {
            log.i { "🔍 Raw listener data: '$data'" }

            // Try multiple parsing strategies for different formats

            // Strategy 1: "SOCKS_PORT=9050 CONTROL_PORT=9051" format
            if (data.contains("SOCKS_PORT=")) {
                val socksMatch = Regex("SOCKS_PORT=(\\d+)").find(data)
                socksMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port ->
                    _socksPort.value = port
                    log.i { "✅ Tor SOCKS port detected (format 1): $port" }
                }
            }

            // Strategy 2: Look for "127.0.0.1:PORT" format for SOCKS
            if (_socksPort.value == null) {
                val socksMatch = Regex("127\\.0\\.0\\.1:(\\d+)").find(data)
                socksMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port ->
                    // Assume first port found is SOCKS (common convention)
                    _socksPort.value = port
                    log.i { "✅ Tor SOCKS port detected (format 2): $port" }
                }
            }

            // Strategy 3: Look for any port number and assume it's SOCKS if we don't have one
            if (_socksPort.value == null) {
                val portMatch = Regex(":(\\d+)").find(data)
                portMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port ->
                    _socksPort.value = port
                    log.i { "✅ Tor SOCKS port detected (format 3): $port" }
                }
            }

            // Control port parsing - try multiple patterns
            if (data.contains("CONTROL_PORT=")) {
                val controlMatch = Regex("CONTROL_PORT=(\\d+)").find(data)
                controlMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port ->
                    _controlPort.value = port
                    log.i { "✅ Tor Control port detected: $port" }
                }
            } else if (data.contains("Control listener listening on")) {
                // Try alternative format: "Control listener listening on 127.0.0.1:9051"
                val controlMatch = Regex("Control listener listening on [^:]+:(\\d+)").find(data)
                controlMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port ->
                    _controlPort.value = port
                    log.i { "✅ Tor Control port detected (alternative format): $port" }
                }
            } else if (data.contains("control") && data.contains("port")) {
                // Log any data that mentions control and port for debugging
                log.d { "🔍 Potential control port data: $data" }
            }

            // Log if we couldn't parse the SOCKS port from listener data
            if (_socksPort.value == null) {
                log.d { "🔍 Could not parse SOCKS port from listener data: '$data'" }
                log.d { "🔍 Will rely on GETINFO command to get the actual port" }
            }

        } catch (e: Exception) {
            log.e(e) { "Failed to parse listener update: $data" }
            log.d { "🔍 Will rely on GETINFO command to get the actual port" }
        }
    }

    /**
     * Debug method to check Tor status and force state updates
     */
    fun debugTorStatus() {
        log.i { "=== TOR DEBUG STATUS ===" }
        log.i { "Current state: ${_torState.value}" }
        log.i { "SOCKS port: ${_socksPort.value}" }
        log.i { "Control port: ${_controlPort.value}" }
        log.i { "Runtime initialized: ${torRuntime != null}" }

        // If we're READY but don't have SOCKS port, try to query it
        if (_torState.value == TorState.READY && _socksPort.value == null) {
            log.w { "🔧 Tor is READY but SOCKS port is null - querying actual port" }
            queryActualSocksPort()
        }

        // Try to trigger some events manually for testing
        if (torRuntime != null && _torState.value != TorState.READY) {
            log.i { "Runtime is available, forcing bootstrap completion..." }

            // Force the state to READY immediately since we have a working SOCKS port
            if (_socksPort.value != null) {
                log.i { "🔧 Forcing Tor state to READY since SOCKS port is available: ${_socksPort.value}" }
                _torState.value = TorState.READY
                log.i { "✅ Tor state forced to READY for bootstrap" }
            } else {
                log.w { "⚠️ Cannot force READY state - no SOCKS port available" }
                // Try to get the port first, then force ready
                queryActualSocksPort()
                // Force ready state after a short delay to allow port query
                launchIO {
                    kotlinx.coroutines.delay(2000)
                    if (_socksPort.value != null) {
                        log.i { "🔧 Forcing Tor state to READY after port query: ${_socksPort.value}" }
                        _torState.value = TorState.READY
                        log.i { "✅ Tor state forced to READY after port detection" }
                    } else {
                        log.e { "❌ Still no SOCKS port after query - cannot force READY state" }
                    }
                }
            }
        }
        log.i { "========================" }
    }

    /**
     * Query the actual SOCKS port from Tor using GETINFO command
     * This is the proper way to get the port information from kmp-tor
     */
    private fun queryActualSocksPort() {
        val runtime = torRuntime
        if (runtime == null) {
            log.e { "❌ Cannot query SOCKS port - Tor runtime not available" }
            fallbackToPortDetection()
            return
        }

        // Add a delay to ensure Tor is fully ready to accept commands
        launchIO {
            kotlinx.coroutines.delay(3000) // Wait 3 seconds for Tor to be fully ready

            log.i { "🔍 Querying actual SOCKS port from Tor using GETINFO command..." }

            try {
                // Use the proper GETINFO command to query the SOCKS listeners
                val getInfoCommand = TorCmd.Info.Get("net/listeners/socks")

                runtime.enqueue(
                    getInfoCommand,
                    OnFailure { error ->
                        log.e { "❌ Failed to query SOCKS port: $error" }
                        log.w { "⚠️ GETINFO failed, falling back to port detection" }
                        fallbackToPortDetection()
                    },
                    OnSuccess { result ->
                        log.i { "✅ GETINFO response received: $result" }
                        parseSocksPortFromGetInfo(result)
                    }
                )
            } catch (e: Exception) {
                log.e(e) { "❌ Exception while querying SOCKS port" }
                log.w { "⚠️ Exception occurred, falling back to port detection" }
                fallbackToPortDetection()
            }
        }
    }

    /**
     * Parse SOCKS port from GETINFO response
     */
    private fun parseSocksPortFromGetInfo(result: Map<String, String>) {
        try {
            val socksListeners = result["net/listeners/socks"]
            log.i { "🔍 Raw SOCKS listeners data: '$socksListeners'" }

            if (socksListeners != null) {
                // Parse the response format: "127.0.0.1:9050" or similar
                val portMatch = Regex("127\\.0\\.0\\.1:(\\d+)").find(socksListeners)
                val port = portMatch?.groupValues?.get(1)?.toIntOrNull()

                if (port != null) {
                    _socksPort.value = port
                    log.i { "✅ Successfully parsed SOCKS port from GETINFO: $port" }

                    // Test connectivity to confirm the port is working
                    launchIO {
                        testSocksConnectivity(port)
                    }
                } else {
                    log.w { "⚠️ Could not parse port from SOCKS listeners: '$socksListeners'" }
                    // Fallback to port detection
                    fallbackToPortDetection()
                }
            } else {
                log.w { "⚠️ No SOCKS listeners data in GETINFO response" }
                // Fallback to port detection
                fallbackToPortDetection()
            }
        } catch (e: Exception) {
            log.e(e) { "❌ Error parsing SOCKS port from GETINFO response" }
            // Fallback to port detection
            fallbackToPortDetection()
        }
    }

    /**
     * Fallback to the old port detection method
     */
    private fun fallbackToPortDetection() {
        log.w { "⚠️ Falling back to port detection method..." }
        launchIO {
            val actualPort = findActualSocksPort()
            if (actualPort != null) {
                _socksPort.value = actualPort
                log.i { "✅ Found SOCKS port via fallback method: $actualPort" }
            } else {
                log.e { "❌ Could not find any accessible SOCKS port" }
                log.w { "⚠️ P2P network connectivity will likely fail" }
            }
        }
    }

    /**
     * Find the actual SOCKS port by testing connectivity (fallback method)
     * Only tests a few common ports as a last resort
     */
    private suspend fun findActualSocksPort(): Int? {
        log.w { "⚠️ Using fallback port detection - this should not be the primary method" }

        // Test only the most common Tor ports as a last resort
        val portsToTest = listOf(9050, 9051, 9052, 9053, 9054)

        for (port in portsToTest) {
            try {
                log.d { "🔍 Testing SOCKS connectivity on port $port..." }

                // Try to create a socket connection to the SOCKS port
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("127.0.0.1", port), 1000) // 1 second timeout
                socket.close()

                log.i { "✅ SOCKS port $port is accessible" }
                return port

            } catch (e: Exception) {
                log.d { "❌ Port $port not accessible: ${e.message}" }
            }
        }

        log.e { "❌ No accessible SOCKS port found - GETINFO command should be used instead" }
        return null
    }

    /**
     * Test SOCKS connectivity on a specific port
     */
    private suspend fun testSocksConnectivity(port: Int) {
        try {
            log.i { "🔍 Testing SOCKS connectivity on configured port $port..." }

            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress("127.0.0.1", port), 2000) // 2 second timeout
            socket.close()

            log.i { "✅ SOCKS proxy is accessible on port $port" }

        } catch (e: Exception) {
            log.e(e) { "❌ SOCKS proxy not accessible on port $port: ${e.message}" }
            log.w { "⚠️ This may cause P2P network connectivity issues" }
        }
    }

    /**
     * Query the actual control port from Tor using GETINFO command
     * This is the proper way to get the control port information from kmp-tor
     */
    fun queryActualControlPort(callback: (Int?) -> Unit) {
        val runtime = torRuntime
        if (runtime == null) {
            log.e { "❌ Cannot query control port - Tor runtime not available" }
            callback(null)
            return
        }

        launchIO {
            log.i { "🔍 Querying actual control port from Tor using GETINFO command..." }

            try {
                // Use the proper GETINFO command to query the control listeners
                val getInfoCommand = TorCmd.Info.Get("net/listeners/control")

                runtime.enqueue(
                    getInfoCommand,
                    OnFailure { error ->
                        log.e { "❌ Failed to query control port: $error" }
                        callback(null)
                    },
                    OnSuccess { result ->
                        log.i { "✅ GETINFO control port response received: $result" }
                        val controlPort = parseControlPortFromGetInfo(result)
                        if (controlPort != null) {
                            _controlPort.value = controlPort
                            log.i { "✅ Successfully parsed control port from GETINFO: $controlPort" }
                        }
                        callback(controlPort)
                    }
                )
            } catch (e: Exception) {
                log.e(e) { "❌ Exception while querying control port" }
                callback(null)
            }
        }
    }

    /**
     * Parse control port from GETINFO response
     */
    private fun parseControlPortFromGetInfo(result: Map<String, String>): Int? {
        return try {
            val controlListeners = result["net/listeners/control"]
            log.i { "🔍 Raw control listeners data: '$controlListeners'" }

            if (controlListeners != null) {
                // Parse the response format: "127.0.0.1:9051" or similar
                val portMatch = Regex("127\\.0\\.0\\.1:(\\d+)").find(controlListeners)
                val port = portMatch?.groupValues?.get(1)?.toIntOrNull()

                if (port != null) {
                    log.i { "✅ Successfully parsed control port from GETINFO: $port" }
                    return port
                } else {
                    log.w { "⚠️ Could not parse port from control listeners: '$controlListeners'" }
                }
            } else {
                log.w { "⚠️ No control listeners data in GETINFO response" }
            }
            null
        } catch (e: Exception) {
            log.e(e) { "❌ Error parsing control port from GETINFO response" }
            null
        }
    }

    /**
     * Set the control port manually (for external mock control servers)
     */
    fun setControlPort(port: Int) {
        _controlPort.value = port
        log.i { "✅ Control port set manually: $port" }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        log.i { "Cleaning up Tor service..." }
        stopTor()
        torRuntime = null
        _torState.value = TorState.STOPPED
        _socksPort.value = null
        _controlPort.value = null
    }
}
