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
    val baseDir: File
) : Logging {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val torService = TorService(context, baseDir)
    private val torConfigManager = TorConfigurationManager(context, baseDir)

    val torState: StateFlow<TorService.TorState> = torService.torState
    val socksPort: StateFlow<Int?> = torService.socksPort
    val controlPort: StateFlow<Int?> = torService.controlPort

    fun getContext(): Context = context

    fun initialize() {
        log.i { "Initializing Tor integration service..." }

        try {
            setupTorConfiguration()
            torService.initialize()
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

                    initialize()
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

            log.e(lastException) { "❌ Failed to initialize Tor after $maxRetries attempts" }
            log.w { "⚠️ App will continue without Tor - users can retry from settings" }
        }
    }

    fun startTorIntegration() {
        log.i { "Starting Tor integration..." }

        serviceScope.launch {
            try {
                torService.startTor()
                log.i { "Tor integration started" }

            } catch (e: Exception) {
                log.e(e) { "Failed to start Tor integration" }
            }
        }
    }

    fun stopTorIntegration() {
        log.i { "Stopping Tor integration..." }

        serviceScope.launch {
            try {
                torService.stopTor()
                log.i { "Tor integration stopped" }

            } catch (e: Exception) {
                log.e(e) { "Failed to stop Tor integration" }
            }
        }
    }

    fun restartTorIntegration() {
        log.i { "Restarting Tor integration..." }
        torService.restartTor()
    }

    fun newTorIdentity() {
        log.i { "Requesting new Tor identity..." }
        torService.newIdentity()
    }

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

    fun getHiddenServiceInfo(): HiddenServiceInfo? {
        val hostname = torConfigManager.getHiddenServiceHostname()
        return if (hostname != null) {
            HiddenServiceInfo(
                hostname = hostname,
                port = 8000,
                isReady = torState.value == TorService.TorState.READY
            )
        } else {
            null
        }
    }

    fun isTorReady(): Boolean {
        return torState.value == TorService.TorState.READY && socksPort.value != null
    }

    fun debugAndFixTorStatus() {
        log.i { "🔧 Debugging and fixing Tor status..." }
        log.i { "Current state: ${torState.value}" }
        log.i { "SOCKS port: ${socksPort.value}" }
        log.i { "Control port: ${controlPort.value}" }
        log.i { "Is ready: ${isTorReady()}" }

        torService.debugTorStatusAndEnsureReadiness()

        log.i { "🔧 After debug - State: ${torState.value}, Port: ${socksPort.value}, Ready: ${isTorReady()}" }
    }

    fun queryActualControlPort(callback: (Int?) -> Unit) {
        log.i { "TorIntegrationService: Querying actual control port from kmp-tor..." }
        torService.queryActualControlPort { controlPort ->
            if (controlPort != null) {
                log.i { "TorIntegrationService: Real control port detected: $controlPort" }
            } else {
                log.w { "TorIntegrationService: Could not detect real control port" }
            }
            callback(controlPort)
        }
    }

    fun setControlPort(port: Int) {
        torService.setControlPort(port)
        log.i { "TorIntegrationService: Control port set to $port" }
    }

    fun getTorStatus(): TorStatus {
        return TorStatus(
            state = torState.value,
            socksPort = socksPort.value,
            controlPort = controlPort.value,
            hiddenServiceHostname = torConfigManager.getHiddenServiceHostname(),
            isReady = isTorReady()
        )
    }

    private fun setupTorConfiguration() {
        log.d { "Setting up Tor configuration..." }

        try {
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

    private fun setupStateMonitoring() {
        serviceScope.launch {
            combine(torState, socksPort) { state, port ->
                Pair(state, port)
            }.collect { (state, port) ->
                handleTorStateChange(state, port)
            }
        }
    }

    private fun handleTorStateChange(state: TorService.TorState, socksPort: Int?) {
        log.d { "Tor state changed: $state, SOCKS port: $socksPort" }

        when (state) {
            TorService.TorState.READY -> {
                if (socksPort != null) {
                    log.i { "Tor is ready - SOCKS proxy available at 127.0.0.1:$socksPort" }

                    torConfigManager.updateBisqConfigForTor(socksPort, enableTor = true)

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
                log.d { "Tor state: $state" }
            }
        }
    }

    fun cleanup() {
        log.i { "Cleaning up Tor integration service..." }

        try {
            torService.cleanup()
            log.i { "Tor integration service cleaned up" }

        } catch (e: Exception) {
            log.e(e) { "Error during Tor integration cleanup" }
        }
    }

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
