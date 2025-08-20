package network.bisq.mobile.client.websocket

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.NetworkUtils.isValidIpv4
import network.bisq.mobile.domain.utils.NetworkUtils.isValidPort
import network.bisq.mobile.domain.utils.NetworkUtils.isValidTorV3Address
import kotlin.concurrent.Volatile

/**
 * Provider to handle dynamic host/port changes
 */
class WebSocketClientProvider(
    private val defaultHost: String,
    private val defaultPort: Int,
    private val settingsRepository: SettingsRepository,
    private val clientFactory: (String, Int) -> WebSocketClient
) : Logging {
    private var observeSettingsJob: Job? = null
    private val mutex = Mutex()

    companion object {
        fun toAddress(uri: String): AddressVO? {
            val trimmed = uri.trim()
            if (trimmed.isBlank()) return null

            // IPv4 or Tor v3 onion: host:port
            val parts = trimmed.split(":", limit = 2)
            if (parts.size != 2) return null

            val rawHost = parts[0].trim()
            val portStr = parts[1].trim()

            if (rawHost.isBlank() || portStr.isBlank()) return null

            val host = if (rawHost.endsWith(".onion")) {
                rawHost.lowercase()
            } else {
                rawHost
            }

            val hostOk = host.isValidIpv4() || host.isValidTorV3Address()
            if (!hostOk) return null

            val port = portStr.toIntOrNull() ?: return null
            if (port !in 1..65535) return null

            return AddressVO(host, port)
        }
    }

    private val ioScope = CoroutineScope(IODispatcher)

    @Volatile
    private var currentClient: WebSocketClient? = null
    private var connectionReady = CompletableDeferred<Boolean>()

    /**
     * Test connection to a new host/port
     */
    suspend fun testClient(host: String, port: Int): Boolean {
        val client = createClient(host, port)
        val url = "ws://$host:$port/websocket"
        return try {
            if (client.isDemo()) {
                ApplicationBootstrapFacade.isDemo = true
                return true
            }
            // Connect and wait a moment to ensure connection is stable
            client.connect(true)
            kotlinx.coroutines.delay(500) // Wait 500ms to ensure connection is stable
            true
        } catch (e: Exception) {
            log.e("Error testing connection to $url: ${e.message}")
            false
        } finally {
            client.disconnect(true)
        }
    }

    private fun createClient(host: String, port: Int): WebSocketClient {
        return clientFactory(host, port)
    }

    // UI usages of this call will have the currentClient available so
    // no need to make it suspend as its used from IO coroutines
    // to be safe never call this method from UI thread
    fun get(): WebSocketClient {
        if (currentClient == null) {
            runBlocking {
                initializeWithSavedSettings()
                launchObserveSettingsChange()
                connectionReady.await()
            }
        }
        return currentClient!!
    }

    /**
     * Initialize the client with saved settings if available, otherwise use defaults
     */
    private suspend fun initializeWithSavedSettings() {
        mutex.withLock {
            if (currentClient == null) {
                // Fetch settings first
                val settings = settingsRepository.fetch()

                // Determine host and port from settings or defaults
                var host = defaultHost
                var port = defaultPort

                settings?.bisqApiUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    val address = toAddress(url);
                    if (address != null) {
                        host = address.host
                        port = address.port
                        log.d { "Using saved settings for trusted node: $host:$port" }
                    } else {
                        log.e { "Error parsing saved URL $url, falling back to defaults" }
                    }
                }

                currentClient = createClient(host, port)
                log.d { "Websocket client initialized with url $host:$port" }

                val connected = try {
                    currentClient?.connect()
                    true
                } catch (e: Exception) {
                    log.e(e) { "Failed to connect to trusted node at $host:$port" }
                    false
                }

                if (connected && !connectionReady.isCompleted) {
                    connectionReady.complete(true)
                }
            }
        }
    }

    /**
     * Launches a coroutine to observe settings changes and update the WebSocket client accordingly.
     */
    private fun launchObserveSettingsChange() {
        if (observeSettingsJob?.isActive == true) {
            log.w { "already observing settings changes" }
            return
        }
        observeSettingsJob = ioScope.launch {
            try {
                settingsRepository.data.collect { newSettings ->
                    mutex.withLock {
                        newSettings?.bisqApiUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            try {
                                val address = toAddress(url)
                                if (address == null) {
                                    log.e { "Error parsing new URL $url" }
                                    return@let
                                }
                                val (newHost, newPort) = address

                                if (isDifferentFromCurrentClient(newHost, newPort)) {
                                    if (currentClient != null) {
                                        log.d { "trusted node changing from ${currentClient!!.host}:${currentClient!!.port} to $newHost:$newPort" }
                                    }
                                    if (currentClient?.isConnected() == true) {
                                        currentClient?.disconnect()
                                    }
                                    currentClient = createClient(newHost, newPort)
                                    log.d { "Websocket client updated with url $newHost:$newPort" }
                                    log.d { "Websocket client - connecting" }
                                    currentClient?.connect()
                                    if (!connectionReady.isCompleted) {
                                        connectionReady.complete(true)
                                    }
                                } else {
                                    if (currentClient?.isConnected() == true) {
                                        log.v { "skip url update, no change" }
                                    } else {
                                        log.v { "url update: no change but found client disconnected" }
                                        currentClient?.let {
                                            log.v { "url update: connecting with existing setup client" }
                                            it.connect()
                                            if (!connectionReady.isCompleted) {
                                                connectionReady.complete(true)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                log.e(e) { "Error connecting to new URL $url" }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "Error updating WebSocket client with new settings." }
            }
        }
    }

    private fun isDifferentFromCurrentClient(host: String, port: Int) =
        currentClient == null || currentClient!!.host != host || currentClient!!.port != port
}