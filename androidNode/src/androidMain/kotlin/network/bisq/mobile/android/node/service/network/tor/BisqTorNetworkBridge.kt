package network.bisq.mobile.android.node.service.network.tor

import android.content.Context
import bisq.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.utils.Logging
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * Bridge between our embedded Tor and Bisq's NetworkService
 * This class handles the integration of our Tor instance with Bisq's networking stack
 */
class BisqTorNetworkBridge(
    private val context: Context,
    private val baseDir: File,
    private val applicationServiceProvider: AndroidApplicationService.Provider
) : Logging {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val torIntegrationService = TorIntegrationService(context, baseDir)

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
