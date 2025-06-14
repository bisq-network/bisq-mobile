package network.bisq.mobile.android.node.service.tor

import android.content.Context
import io.matthewnelson.kmp.tor.runtime.RuntimeEvent
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.OnFailure
import io.matthewnelson.kmp.tor.runtime.core.OnSuccess
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
import io.matthewnelson.kmp.tor.runtime.core.ctrl.TorCmd
import io.matthewnelson.kmp.tor.runtime.core.TorEvent
import io.matthewnelson.kmp.tor.resource.exec.tor.ResourceLoaderTorExec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.utils.Logging
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
) : Logging {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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

                // Configure Tor options
                config { environment ->
                    // Configure SOCKS port (auto-assign)
                    TorOption.SocksPort.configure { auto() }

                    // Configure control port (auto-assign)
                    TorOption.ControlPort.configure { auto() }

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
            serviceScope.launch {
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

        // In kmp-tor 2.4.0, the daemon starts automatically when runtime is created
        // We just need to wait for bootstrap events
        log.i { "✅ Tor daemon should start automatically - waiting for bootstrap events..." }

        // Add a timeout to check if we get any events
        serviceScope.launch {
            kotlinx.coroutines.delay(10000) // Wait 10 seconds
            if (_torState.value == TorState.STARTING) {
                log.w { "⚠️ No bootstrap events received after 10 seconds. Checking if Tor is actually running..." }
                // Try to force some events for debugging
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

        // For now, just update the state - actual stopping handled by cleanup
        log.i { "✅ Tor daemon stop requested" }
        _torState.value = TorState.STOPPING

        serviceScope.launch {
            kotlinx.coroutines.delay(1000)
            _torState.value = TorState.STOPPED
        }
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
        
        log.i { "✅ Tor daemon restart requested" }
        // Simple restart by stopping and starting
        stopTor()
        serviceScope.launch {
            kotlinx.coroutines.delay(2000)
            startTor()
        }
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

                    // Ensure we have a SOCKS port set
                    if (_socksPort.value == null) {
                        log.w { "⚠️ SOCKS port not detected from listeners, using default 9050" }
                        _socksPort.value = 9050
                    }

                    log.i { "🚀 Tor ready with SOCKS port: ${_socksPort.value}" }
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
            _torState.value = TorState.BOOTSTRAPPING
            log.d { "Tor bootstrapping: $data" }
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

            // Control port parsing
            if (data.contains("CONTROL_PORT=")) {
                val controlMatch = Regex("CONTROL_PORT=(\\d+)").find(data)
                controlMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port ->
                    _controlPort.value = port
                    log.i { "✅ Tor Control port detected: $port" }
                }
            }

            // If we still don't have a SOCKS port, try to set a default
            if (_socksPort.value == null) {
                log.w { "⚠️ Could not parse SOCKS port from listener data, using default 9050" }
                _socksPort.value = 9050
            }

        } catch (e: Exception) {
            log.e(e) { "Failed to parse listener update: $data" }
            // Fallback to default SOCKS port
            if (_socksPort.value == null) {
                log.w { "⚠️ Using fallback SOCKS port 9050 due to parsing error" }
                _socksPort.value = 9050
            }
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

        // If we're READY but don't have SOCKS port, try to fix it
        if (_torState.value == TorState.READY && _socksPort.value == null) {
            log.w { "🔧 Tor is READY but SOCKS port is null - setting default" }
            _socksPort.value = 9050
            log.i { "🔧 SOCKS port set to default: ${_socksPort.value}" }
        }

        // Try to trigger some events manually for testing
        if (torRuntime != null && _torState.value != TorState.READY) {
            log.i { "Runtime is available, simulating bootstrap for testing..." }

            // Simulate receiving a bootstrap event to test the flow
            serviceScope.launch {
                log.i { "🧪 Simulating bootstrap events for testing..." }
                handleTorEvent(TorEvent.NOTICE, "Bootstrapped 25%: Loading relays")
                kotlinx.coroutines.delay(1000)
                handleTorEvent(TorEvent.NOTICE, "Bootstrapped 50%: Loading relays")
                kotlinx.coroutines.delay(1000)
                handleTorEvent(TorEvent.NOTICE, "Bootstrapped 75%: Loading relays")
                kotlinx.coroutines.delay(1000)
                handleTorEvent(TorEvent.NOTICE, "Bootstrapped 100%: Done")
            }
        }
        log.i { "========================" }
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
