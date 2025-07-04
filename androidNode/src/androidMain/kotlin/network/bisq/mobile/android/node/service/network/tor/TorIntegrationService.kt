package network.bisq.mobile.android.node.service.network.tor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.utils.Logging
import java.io.File

/**
 * Main integration service that coordinates Tor functionality with Bisq Mobile
 * This service manages the lifecycle and integration between Tor and Bisq networking
 */
class TorIntegrationService(
    private val context: Context,
    private val baseDir: File
) : Logging {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val torService = TorService(context, baseDir)
    private val torConfigManager = TorConfigurationManager(context, baseDir)
    
    // Expose Tor state and configuration
    val torState: StateFlow<TorService.TorState> = torService.torState
    val socksPort: StateFlow<Int?> = torService.socksPort
    val controlPort: StateFlow<Int?> = torService.controlPort

    /**
     * Get the Android context for file operations
     */
    fun getContext(): Context = context

    fun initialize() {
        log.i { "Initializing Tor integration service..." }

        try {
            // Initialize Tor configuration
            setupTorConfiguration()

            // Initialize Tor service
            torService.initialize()

            // Set up state monitoring
            setupStateMonitoring()

            log.i { "Tor integration service initialized successfully" }

        } catch (e: Exception) {
            log.e(e) { "Failed to initialize Tor integration service" }
            throw e
        }
    }

    fun initializeAndStart(
        maxRetries: Int = 3,
        retryDelayMs: Long = 5000
    ) {
        log.i { "🚀 Starting production Tor integration (max retries: $maxRetries)..." }

        serviceScope.launch {
            var attempt = 0
            var lastException: Exception? = null

            while (attempt < maxRetries) {
                try {
                    attempt++
                    log.i { "Tor initialization attempt $attempt/$maxRetries" }

                    // Initialize Tor integration
                    initialize()

                    // Start Tor daemon
                    startTorIntegration()

                    log.i { "✅ Tor integration started successfully on attempt $attempt" }
                    return@launch

                } catch (e: Exception) {
                    lastException = e
                    log.w(e) { "Tor initialization attempt $attempt failed" }

                    if (attempt < maxRetries) {
                        log.i { "⏳ Retrying in ${retryDelayMs}ms..." }
                        kotlinx.coroutines.delay(retryDelayMs)
                    }
                }
            }

            // All attempts failed
            log.e(lastException) { "❌ Failed to initialize Tor after $maxRetries attempts" }
            log.w { "⚠️ App will continue without Tor - users can retry from settings" }
        }
    }

    /**
     * Start Tor and integrate with Bisq networking
     */
    fun startTorIntegration() {
        log.i { "Starting Tor integration..." }
        
        serviceScope.launch {
            try {
                // Start Tor daemon
                torService.startTor()
                
                log.i { "Tor integration started" }
                
            } catch (e: Exception) {
                log.e(e) { "Failed to start Tor integration" }
            }
        }
    }

    /**
     * Stop Tor integration
     */
    fun stopTorIntegration() {
        log.i { "Stopping Tor integration..." }
        
        serviceScope.launch {
            try {
                // Stop Tor daemon
                torService.stopTor()
                
                log.i { "Tor integration stopped" }
                
            } catch (e: Exception) {
                log.e(e) { "Failed to stop Tor integration" }
            }
        }
    }

    /**
     * Restart Tor integration
     */
    fun restartTorIntegration() {
        log.i { "Restarting Tor integration..." }
        torService.restartTor()
    }

    /**
     * Request new Tor identity
     */
    fun newTorIdentity() {
        log.i { "Requesting new Tor identity..." }
        torService.newIdentity()
    }

    /**
     * Get current SOCKS proxy configuration for Bisq
     */
    fun getSocksProxyConfig(): SocksProxyConfig? {
        val port = socksPort.value
        return if (port != null) {
            SocksProxyConfig(
                host = "127.0.0.1",
                port = port,
                enabled = torState.value == TorService.TorState.READY
            )
        } else {
            null
        }
    }

    /**
     * Get hidden service information
     */
    fun getHiddenServiceInfo(): HiddenServiceInfo? {
        val hostname = torConfigManager.getHiddenServiceHostname()
        return if (hostname != null) {
            HiddenServiceInfo(
                hostname = hostname,
                port = 8000, // Default Bisq node port
                isReady = torState.value == TorService.TorState.READY
            )
        } else {
            null
        }
    }

    /**
     * Check if Tor is ready for Bisq networking
     */
    fun isTorReady(): Boolean {
        return torState.value == TorService.TorState.READY && socksPort.value != null
    }

    /**
     * Debug Tor status and attempt to fix common issues
     */
    fun debugAndFixTorStatus() {
        log.i { "🔧 Debugging and fixing Tor status..." }
        log.i { "Current state: ${torState.value}" }
        log.i { "SOCKS port: ${socksPort.value}" }
        log.i { "Control port: ${controlPort.value}" }
        log.i { "Is ready: ${isTorReady()}" }

        // Call the TorService debug method
        torService.debugTorStatus()

        // Log the status after debug
        log.i { "🔧 After debug - State: ${torState.value}, Port: ${socksPort.value}, Ready: ${isTorReady()}" }
    }

    /**
     * Query the actual control port from Tor using GETINFO command
     * This is the proper way to get the control port information from kmp-tor
     */
    fun queryActualControlPort(callback: (Int?) -> Unit) {
        log.i { "🔍 TorIntegrationService: Querying actual control port from kmp-tor..." }
        torService.queryActualControlPort { controlPort ->
            if (controlPort != null) {
                log.i { "✅ TorIntegrationService: Real control port detected: $controlPort" }
            } else {
                log.w { "⚠️ TorIntegrationService: Could not detect real control port" }
            }
            callback(controlPort)
        }
    }

    /**
     * Set the control port manually (for external mock control servers)
     */
    fun setControlPort(port: Int) {
        torService.setControlPort(port)
        log.i { "✅ TorIntegrationService: Control port set to $port" }
    }

    /**
     * Get Tor status information
     */
    fun getTorStatus(): TorStatus {
        return TorStatus(
            state = torState.value,
            socksPort = socksPort.value,
            controlPort = controlPort.value,
            hiddenServiceHostname = torConfigManager.getHiddenServiceHostname(),
            isReady = isTorReady()
        )
    }

    /**
     * Setup Tor configuration
     */
    private fun setupTorConfiguration() {
        log.d { "Setting up Tor configuration..." }
        
        try {
            // Generate default configuration if it doesn't exist
            if (!torConfigManager.hasTorConfig()) {
                val defaultConfig = torConfigManager.getDefaultTorConfig()
                torConfigManager.writeTorConfig(defaultConfig)
                log.i { "Generated default Tor configuration" }
            } else {
                log.i { "Using existing Tor configuration" }
            }
            
        } catch (e: Exception) {
            log.e(e) { "Failed to setup Tor configuration" }
            throw e
        }
    }

    /**
     * Setup state monitoring to coordinate with Bisq
     */
    private fun setupStateMonitoring() {
        serviceScope.launch {
            // Monitor Tor state and SOCKS port changes
            combine(torState, socksPort) { state, port ->
                Pair(state, port)
            }.collect { (state, port) ->
                handleTorStateChange(state, port)
            }
        }
    }

    /**
     * Handle Tor state changes and update Bisq configuration accordingly
     */
    private fun handleTorStateChange(state: TorService.TorState, socksPort: Int?) {
        log.d { "Tor state changed: $state, SOCKS port: $socksPort" }
        
        when (state) {
            TorService.TorState.READY -> {
                if (socksPort != null) {
                    log.i { "Tor is ready - SOCKS proxy available at 127.0.0.1:$socksPort" }
                    
                    // Update Bisq configuration to use Tor
                    torConfigManager.updateBisqConfigForTor(socksPort, enableTor = true)
                    
                    // Log hidden service information if available
                    val hiddenServiceInfo = getHiddenServiceInfo()
                    if (hiddenServiceInfo != null) {
                        log.i { "Hidden service available: ${hiddenServiceInfo.hostname}:${hiddenServiceInfo.port}" }
                    }
                }
            }
            
            TorService.TorState.ERROR -> {
                log.w { "Tor encountered an error - falling back to clear net" }
                torConfigManager.updateBisqConfigForTor(0, enableTor = false)
            }
            
            TorService.TorState.STOPPED -> {
                log.i { "Tor stopped - using clear net" }
                torConfigManager.updateBisqConfigForTor(0, enableTor = false)
            }
            
            else -> {
                // Handle other states (STARTING, BOOTSTRAPPING, STOPPING)
                log.d { "Tor state: $state" }
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        log.i { "Cleaning up Tor integration service..." }
        
        try {
            torService.cleanup()
            // Note: We don't clean up configuration by default to preserve settings
            
            log.i { "Tor integration service cleaned up" }
            
        } catch (e: Exception) {
            log.e(e) { "Error during Tor integration cleanup" }
        }
    }

    /**
     * Force cleanup including configuration (for testing/debugging)
     */
    fun forceCleanup() {
        log.w { "Force cleaning up Tor integration service..." }
        
        try {
            torService.cleanup()
            torConfigManager.cleanup()
            
            log.i { "Tor integration service force cleaned up" }
            
        } catch (e: Exception) {
            log.e(e) { "Error during Tor integration force cleanup" }
        }
    }

    /**
     * Data classes for Tor information
     */
    data class SocksProxyConfig(
        val host: String,
        val port: Int,
        val enabled: Boolean
    )

    data class HiddenServiceInfo(
        val hostname: String,
        val port: Int,
        val isReady: Boolean
    )

    data class TorStatus(
        val state: TorService.TorState,
        val socksPort: Int?,
        val controlPort: Int?,
        val hiddenServiceHostname: String?,
        val isReady: Boolean
    )
}
