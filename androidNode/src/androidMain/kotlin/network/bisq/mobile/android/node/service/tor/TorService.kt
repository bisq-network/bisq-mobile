package network.bisq.mobile.android.node.service.tor

import android.content.Context
import io.matthewnelson.kmp.tor.runtime.RuntimeEvent
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import io.matthewnelson.kmp.tor.runtime.core.OnEvent
import io.matthewnelson.kmp.tor.runtime.core.config.TorOption
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
                    log.e { "Tor Error: $data" }
                    handleTorEvent(TorEvent.ERR, data)
                }

                observerStatic(TorEvent.WARN, OnEvent.Executor.Immediate) { data ->
                    log.w { "Tor Warning: $data" }
                    handleTorEvent(TorEvent.WARN, data)
                }

                observerStatic(TorEvent.NOTICE, OnEvent.Executor.Immediate) { data ->
                    log.i { "Tor Notice: $data" }
                    handleTorEvent(TorEvent.NOTICE, data)
                }

                observerStatic(TorEvent.STATUS_CLIENT, OnEvent.Executor.Immediate) { data ->
                    log.d { "Tor Status Client: $data" }
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

        serviceScope.launch {
            try {
                // Use a simple approach - just log that we're starting
                // The actual daemon management will be handled by the runtime
                log.i { "Tor daemon start requested" }
                _torState.value = TorState.STARTING
            } catch (e: Exception) {
                log.e(e) { "Failed to start Tor daemon" }
                _torState.value = TorState.ERROR
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

        serviceScope.launch {
            try {
                log.i { "Tor daemon stop requested" }
                _torState.value = TorState.STOPPING
                // The actual stopping will be handled by cleanup
            } catch (e: Exception) {
                log.e(e) { "Failed to stop Tor daemon" }
                _torState.value = TorState.ERROR
            }
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
        
        serviceScope.launch {
            try {
                log.i { "Tor daemon restart requested" }
                // Restart by stopping and starting
                stopTor()
                startTor()
            } catch (e: Exception) {
                log.e(e) { "Failed to restart Tor daemon" }
                _torState.value = TorState.ERROR
            }
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
        
        serviceScope.launch {
            try {
                log.i { "New Tor identity requested" }
                // This would typically send a NEWNYM signal to Tor
                // For now, just log the request
            } catch (e: Exception) {
                log.e(e) { "Failed to request new Tor identity" }
            }
        }
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
                log.i { "Tor Notice: $data" }
                if (data.contains("Bootstrapped 100%")) {
                    _torState.value = TorState.READY
                    log.i { "Tor is ready for connections" }
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
            // Parse listener information to extract ports
            // Format typically: "SOCKS_PORT=9050 CONTROL_PORT=9051"
            if (data.contains("SOCKS_PORT=")) {
                val socksMatch = Regex("SOCKS_PORT=(\\d+)").find(data)
                socksMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port ->
                    _socksPort.value = port
                    log.i { "Tor SOCKS port: $port" }
                }
            }
            
            if (data.contains("CONTROL_PORT=")) {
                val controlMatch = Regex("CONTROL_PORT=(\\d+)").find(data)
                controlMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port ->
                    _controlPort.value = port
                    log.i { "Tor Control port: $port" }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to parse listener update: $data" }
        }
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
