package network.bisq.mobile.android.node.service.tor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.utils.Logging
import java.io.File

/**
 * Testing service to validate Tor integration without full Bisq networking
 */
class TorTestingService(
    private val context: Context,
    private val baseDir: File
) : Logging {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val torIntegrationService = TorIntegrationService(context, baseDir)

    /**
     * Test basic Tor functionality
     */
    fun testBasicTorFunctionality() {
        log.i { "Starting basic Tor functionality test..." }
        
        serviceScope.launch {
            try {
                // Initialize Tor
                torIntegrationService.initialize()
                
                // Start Tor
                torIntegrationService.startTorIntegration()
                
                // Monitor Tor state
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

    /**
     * Test Tor connectivity by checking SOCKS proxy
     */
    private suspend fun testTorConnectivity() {
        try {
            val socksConfig = torIntegrationService.getSocksProxyConfig()
            if (socksConfig != null) {
                log.i { "✅ SOCKS proxy available at ${socksConfig.host}:${socksConfig.port}" }
                
                // Test hidden service if available
                val hiddenService = torIntegrationService.getHiddenServiceInfo()
                if (hiddenService != null) {
                    log.i { "✅ Hidden service available: ${hiddenService.hostname}:${hiddenService.port}" }
                } else {
                    log.w { "⚠️ Hidden service not yet available" }
                }
                
                // Log Tor status
                val status = torIntegrationService.getTorStatus()
                log.i { "📊 Tor Status: $status" }
                
            } else {
                log.e { "❌ SOCKS proxy not available" }
            }
            
        } catch (e: Exception) {
            log.e(e) { "Failed to test Tor connectivity" }
        }
    }

    /**
     * Test Tor configuration
     */
    fun testTorConfiguration() {
        log.i { "Testing Tor configuration..." }
        
        try {
            val configManager = TorConfigurationManager(context, baseDir)
            
            // Test configuration generation
            val config = configManager.getDefaultTorConfig()
            log.i { "✅ Generated Tor configuration (${config.length} chars)" }
            
            // Test configuration validation
            val isValid = configManager.validateTorConfig(config)
            log.i { "✅ Configuration validation: $isValid" }
            
            // Test configuration writing
            configManager.writeTorConfig(config)
            log.i { "✅ Configuration written to file" }
            
            // Test configuration reading
            val readConfig = configManager.readTorConfig()
            log.i { "✅ Configuration read from file: ${readConfig != null}" }
            
        } catch (e: Exception) {
            log.e(e) { "Failed to test Tor configuration" }
        }
    }

    /**
     * Comprehensive test suite
     */
    fun runComprehensiveTest() {
        log.i { "🧪 Running comprehensive Tor test suite..." }
        
        serviceScope.launch {
            try {
                // Test 1: Configuration
                testTorConfiguration()
                delay(1000)
                
                // Test 2: Basic functionality
                testBasicTorFunctionality()
                
                // Test 3: Wait for bootstrap and test connectivity
                // This will be handled by the state monitoring in testBasicTorFunctionality
                
            } catch (e: Exception) {
                log.e(e) { "Comprehensive test failed" }
            }
        }
    }

    /**
     * Stop testing and cleanup
     */
    fun stopTesting() {
        log.i { "Stopping Tor testing..." }
        torIntegrationService.cleanup()
    }
}
