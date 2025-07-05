package network.bisq.mobile.android.node.service.network.tor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.utils.Logging
import java.io.File

/**
 * Testing service used to validate Tor integration without full Bisq networking
 *
 * ⚠️ FOR TESTING AND VERIFICATION PURPOSES ONLY ⚠️
 *
 * This service is used to manually test and verify that the Tor daemon integration
 * is working correctly. It provides methods to:
 * - Test Tor configuration generation and validation
 * - Test Tor daemon startup and bootstrap process
 * - Test SOCKS proxy connectivity
 * - Test hidden service functionality
 * - Debug Tor status and troubleshoot issues
 *
 * This service should NOT be used in production. For production Tor integration,
 * use TorIntegrationService directly from MainApplication::onCreate().
 *
 * Usage: Call methods manually for testing/debugging purposes only.
 */
class TorTestingService(
    private val context: Context,
    private val baseDir: File
) : Logging {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val torIntegrationService = TorIntegrationService(context, baseDir)

    fun testBasicTorFunctionality() {
        log.i { "Starting basic Tor functionality test..." }

        serviceScope.launch {
            try {
                torIntegrationService.initialize()
                torIntegrationService.startTorIntegration()

                torIntegrationService.torState.collect { state ->
                    log.i { "Tor state: $state" }

                    when (state) {
                        TorService.TorState.READY -> {
                            log.i { "✅ Tor is ready!" }
                            testTorConnectivity()
                        }
                        TorService.TorState.ERROR -> {
                            log.e { "❌ Tor encountered an error" }
                        }
                        TorService.TorState.BOOTSTRAPPING -> {
                            log.i { "🔄 Tor is bootstrapping..." }
                        }
                        else -> {
                            log.d { "Tor state: $state" }
                        }
                    }
                }

            } catch (e: Exception) {
                log.e(e) { "Failed to test Tor functionality" }
            }
        }
    }

    private suspend fun testTorConnectivity() {
        try {
            val socksConfig = torIntegrationService.getSocksProxyConfig()
            if (socksConfig != null) {
                log.i { "✅ SOCKS proxy available at ${socksConfig.host}:${socksConfig.port}" }

                val hiddenService = torIntegrationService.getHiddenServiceInfo()
                if (hiddenService != null) {
                    log.i { "✅ Hidden service available: ${hiddenService.hostname}:${hiddenService.port}" }
                } else {
                    log.w { "⚠️ Hidden service not yet available" }
                }

                val status = torIntegrationService.getTorStatus()
                log.i { "📊 Tor Status: $status" }

            } else {
                log.e { "❌ SOCKS proxy not available" }
            }

        } catch (e: Exception) {
            log.e(e) { "Failed to test Tor connectivity" }
        }
    }

    fun testTorConfiguration() {
        log.i { "Testing Tor configuration..." }

        try {
            val configManager = TorConfigurationManager(context, baseDir)

            val config = configManager.getDefaultTorConfig()
            log.i { "✅ Generated Tor configuration (${config.length} chars)" }

            val isValid = configManager.validateTorConfig(config)
            log.i { "✅ Configuration validation: $isValid" }

            configManager.writeTorConfig(config)
            log.i { "✅ Configuration written to file" }

            val readConfig = configManager.readTorConfig()
            log.i { "✅ Configuration read from file: ${readConfig != null}" }

        } catch (e: Exception) {
            log.e(e) { "Failed to test Tor configuration" }
        }
    }

    fun runComprehensiveTest() {
        log.i { "🧪 Running comprehensive Tor test suite..." }

        serviceScope.launch {
            try {
                testTorConfiguration()
                delay(1000)
                testBasicTorFunctionality()

            } catch (e: Exception) {
                log.e(e) { "Comprehensive test failed" }
            }
        }
    }

    fun debugTorStatus() {
        log.i { "🔍 Debugging Tor status..." }

        try {
            log.i { "Current Tor state: ${torIntegrationService.torState.value}" }
            log.i { "SOCKS port: ${torIntegrationService.socksPort.value}" }
            log.i { "Control port: ${torIntegrationService.controlPort.value}" }
            log.i { "Is Tor ready: ${torIntegrationService.isTorReady()}" }

            val status = torIntegrationService.getTorStatus()
            log.i { "Full status: $status" }

        } catch (e: Exception) {
            log.e(e) { "Failed to debug Tor status" }
        }
    }

    fun stopTesting() {
        log.i { "Stopping Tor testing..." }
        torIntegrationService.cleanup()
    }
}
