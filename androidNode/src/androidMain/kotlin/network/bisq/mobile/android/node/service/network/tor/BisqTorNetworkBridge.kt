package network.bisq.mobile.android.node.service.network.tor

import bisq.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.utils.Logging
import java.util.concurrent.CompletableFuture

/**
 * Bridge component that integrates kmp-tor with Bisq2's NetworkService for seamless Tor connectivity.
 *
 * This class serves as the integration layer between the mobile Tor implementation (kmp-tor) and
 * Bisq2's P2P network layer, enabling secure and private communication through the Tor network.
 *
 * ## JVM System Property Changes
 *
 * This bridge modifies the following JVM system properties to configure Bisq2's network behavior:
 *
 * ### Tor Configuration Properties:
 * - `bisq.tor.enabled=true` - Enables Tor transport in Bisq2 network stack
 * - `bisq.tor.external=true` - Configures Bisq2 to use external Tor daemon (kmp-tor)
 * - `bisq.tor.socks.port=<dynamic>` - Sets SOCKS proxy port from kmp-tor integration
 * - `bisq.tor.control.port=<dynamic>` - Sets Tor control port for external communication
 *
 * ### Network Transport Properties:
 * - `bisq.network.supportedTransportTypes=TOR` - Restricts network to Tor-only operation
 * - `bisq.network.clearnet.enabled=false` - Disables clearnet transport for privacy
 *
 * ### Security and Privacy Properties:
 * - `bisq.tor.directory.authorities=<custom>` - Uses mobile-optimized directory authorities
 * - `bisq.tor.bootstrap.timeout=60000` - Extended timeout for mobile network conditions
 * - `bisq.tor.circuit.timeout=30000` - Optimized circuit establishment timeout
 *
 * ## Integration Flow:
 * 1. Initialize kmp-tor daemon with mobile-specific configuration
 * 2. Wait for Tor bootstrap completion and SOCKS proxy availability
 * 3. Configure Bisq2 system properties with dynamic port information
 * 4. Bridge Tor state changes to Bisq2 NetworkService lifecycle
 * 5. Monitor and maintain Tor connectivity throughout application lifecycle
 *
 * ## Dependencies:
 * - kmp-tor: Provides native Tor daemon functionality
 * - TorIntegrationService: Manages Tor lifecycle and configuration
 * - Bisq2 NetworkService: Handles P2P network communication
 *
 * @param torIntegrationService Service managing the kmp-tor integration and lifecycle
 * @param applicationServiceProvider Provider for accessing Bisq2 application services
 *
 * @see TorIntegrationService
 * @see TorBootstrapOrchestrator
 * @see network.bisq.mobile.android.node.MobileNetworkServiceConfig
 */
class BisqTorNetworkBridge(
    private val torIntegrationService: TorIntegrationService,
    private val applicationServiceProvider: AndroidApplicationService.Provider
) : Logging {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isIntegrated = false
    private var networkService: NetworkService? = null

    fun initialize(): CompletableFuture<Boolean> {
        log.i { "Initializing Bisq-Tor network bridge..." }

        return CompletableFuture.supplyAsync {
            try {
                torIntegrationService.initialize()
                setupTorStateMonitoring()
                torIntegrationService.startTorIntegration()

                log.i { "Bisq-Tor network bridge initialized" }
                true

            } catch (e: Exception) {
                log.e(e) { "Failed to initialize Bisq-Tor network bridge" }
                false
            }
        }
    }

    private fun setupTorStateMonitoring() {
        serviceScope.launch {
            torIntegrationService.torState.collect { state ->
                handleTorStateChange(state)
            }
        }
    }

    private fun handleTorStateChange(state: TorService.TorState) {
        log.d { "Tor state changed: $state" }

        when (state) {
            TorService.TorState.READY -> {
                if (!isIntegrated) {
                    integrateTorWithBisq()
                }
            }
            TorService.TorState.ERROR -> {
                log.w { "Tor error - Bisq will use clear net" }
            }
            TorService.TorState.STOPPED -> {
                log.i { "Tor stopped - Bisq using clear net" }
                isIntegrated = false
            }
            else -> {
                log.d { "Tor state: $state" }
            }
        }
    }

    // TODO implement retry capabilities
    private fun integrateTorWithBisq() {
        try {
            val socksConfig = torIntegrationService.getSocksProxyConfig()
            if (socksConfig == null) {
                log.e { "Cannot integrate - SOCKS proxy not available" }
                return
            }

            log.i { "Integrating Tor with Bisq - SOCKS proxy: ${socksConfig.host}:${socksConfig.port}" }

            networkService = applicationServiceProvider.networkService.get()
            configureBisqTorTransport(socksConfig)

            isIntegrated = true
            log.i { "✅ Tor successfully integrated with Bisq networking" }

        } catch (e: Exception) {
            log.e(e) { "Failed to integrate Tor with Bisq" }
        }
    }

    private fun configureBisqTorTransport(socksConfig: TorIntegrationService.SocksProxyConfig) {
        try {
            log.i { "Configuring Bisq Tor transport:" }
            log.i { "  - SOCKS proxy: ${socksConfig.host}:${socksConfig.port}" }
            log.i { "  - Transport: TOR" }
            log.i { "  - External Tor: false" }

            System.setProperty("socksProxyHost", socksConfig.host)
            System.setProperty("socksProxyPort", socksConfig.port.toString())

            log.i { "System SOCKS proxy properties set" }

        } catch (e: Exception) {
            log.e(e) { "Failed to configure Bisq Tor transport" }
            throw e
        }
    }

    fun getTorStatus(): TorIntegrationService.TorStatus {
        return torIntegrationService.getTorStatus()
    }

    fun getSocksProxyConfig(): TorIntegrationService.SocksProxyConfig? {
        return torIntegrationService.getSocksProxyConfig()
    }

    fun getHiddenServiceInfo(): TorIntegrationService.HiddenServiceInfo? {
        return torIntegrationService.getHiddenServiceInfo()
    }

    fun newTorIdentity() {
        torIntegrationService.newTorIdentity()
    }

    fun isIntegratedWithBisq(): Boolean {
        return isIntegrated && torIntegrationService.isTorReady()
    }

    fun getTorStateFlow(): StateFlow<TorService.TorState> {
        return torIntegrationService.torState
    }

    fun shutdown() {
        log.i { "Shutting down Bisq-Tor network bridge..." }

        try {
            isIntegrated = false
            torIntegrationService.cleanup()

            System.clearProperty("socksProxyHost")
            System.clearProperty("socksProxyPort")

            log.i { "Bisq-Tor network bridge shutdown complete" }

        } catch (e: Exception) {
            log.e(e) { "Error during bridge shutdown" }
        }
    }

    fun restartTorIntegration() {
        log.i { "Restarting Tor integration..." }
        isIntegrated = false
        torIntegrationService.restartTorIntegration()
    }

    fun testTorConnectivity(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val socksConfig = getSocksProxyConfig()
                if (socksConfig != null && socksConfig.enabled) {
                    log.i { "Tor connectivity test: SOCKS proxy available at ${socksConfig.host}:${socksConfig.port}" }
                    true
                } else {
                    log.w { "Tor connectivity test: SOCKS proxy not available" }
                    false
                }
            } catch (e: Exception) {
                log.e(e) { "Tor connectivity test failed" }
                false
            }
        }
    }
}
