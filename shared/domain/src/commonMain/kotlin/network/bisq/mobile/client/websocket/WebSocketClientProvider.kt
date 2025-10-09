package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging
import kotlin.concurrent.Volatile

/**
 * Provider to handle dynamic host/port changes
 */
class WebSocketClientProvider(
    private val defaultHost: String,
    private val defaultPort: Int,
    private val settingsRepository: SettingsRepository,
    private val httpClient: HttpClient,
    private val webSocketClientFactory: WebSocketClientFactory
) : Logging {
    private var observeSettingsJob: Job? = null
    private val mutex = Mutex()
    private val getMutex = Mutex()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState = _connectionState.asStateFlow()

    private var stateCollectionJob: Job? = null

    private val ioScope = CoroutineScope(IODispatcher)

    @Volatile
    private var currentClient: WebSocketClient? = null

    /**
     * Test connection to a new host/port
     */
    suspend fun testClient(host: String, port: Int, timeout: Long = 15000L): Throwable? {
        val client = createClient(host, port)
        try {
            val error = client.connect(timeout)
            if (error == null) {
                // Wait 500ms to ensure connection is stable
                kotlinx.coroutines.delay(500)
            } else {
                log.e(error) { "Error testing connection to ws://$host:$port/websocket" }
            }
            return error
        } finally {
            client.disconnect()
        }
    }

    private fun createClient(host: String, port: Int): WebSocketClient {
        return webSocketClientFactory.createNewClient(httpClient, host, port)
    }

    /**
     * gets the websocket client if already exists, or
     * creates the websocket client and connects to server according to saved settings and awaits it
     */
    suspend fun get(): WebSocketClient {
        getMutex.withLock {
            currentClient?.let {
                return it
            }
            val newClient = initializeWithSavedSettings() // waits for connection results
            launchObserveSettingsChange()
            return newClient
        }
    }

    /**
     * Initialize the client with saved settings if available, otherwise use defaults
     */
    private suspend fun initializeWithSavedSettings(): WebSocketClient {
        mutex.withLock {
            currentClient?.let {
                return it
            }

            // Fetch settings first
            val settings = settingsRepository.fetch()

            // Determine host and port from settings or defaults
            var host = defaultHost
            var port = defaultPort

            settings.bisqApiUrl.takeIf { it.isNotBlank() }?.let { url ->
                val address = AddressVO.from(url);
                if (address != null) {
                    host = address.host
                    port = address.port
                    log.d { "Using saved settings for trusted node: $host:$port" }
                } else {
                    log.e { "Error parsing saved URL $url, falling back to defaults" }
                }
            }

            val newClient = createClient(host, port)
            currentClient = newClient
            ApplicationBootstrapFacade.isDemo = newClient is WebSocketClientDemo
            stateCollectionJob?.cancel()
            stateCollectionJob = ioScope.launch {
                newClient.webSocketClientStatus.collect {
                    _connectionState.value = it
                }
            }
            log.d { "WebSocket client initialized with url $host:$port" }

            newClient.connect()

            return newClient

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
                        newSettings.bisqApiUrl.takeIf { it.isNotBlank() }?.let { url ->
                            val address = AddressVO.from(url)
                            if (address == null) {
                                log.e { "Error parsing new URL $url" }
                                return@let
                            }
                            val (newHost, newPort) = address

                            var error: Throwable? = null
                            if (isDifferentFromCurrentClient(newHost, newPort)) {
                                if (currentClient != null) {
                                    log.d { "trusted node changing from ${currentClient!!.host}:${currentClient!!.port} to $newHost:$newPort" }
                                }
                                if (currentClient?.isConnected() == true) {
                                    currentClient?.disconnect()
                                }
                                val newClient = createClient(newHost, newPort)
                                currentClient = newClient
                                stateCollectionJob?.cancel()
                                stateCollectionJob = ioScope.launch {
                                    newClient.webSocketClientStatus.collect {
                                        _connectionState.value = it
                                    }
                                }
                                ApplicationBootstrapFacade.isDemo = newClient is WebSocketClientDemo
                                log.d { "WebSocket client updated with url $newHost:$newPort" }
                                log.d { "WebSocket client - connecting" }
                                error = newClient.connect()
                            } else {
                                if (currentClient?.isConnected() == true) {
                                    log.v { "skip url update, no change" }
                                } else {
                                    log.v { "url update: no change but found client disconnected" }
                                    currentClient?.let {
                                        log.v { "url update: connecting with existing setup client" }
                                        error = it.connect()
                                    }
                                }
                            }
                            if (error != null) {
                                log.e(error) { "Error connecting to new URL $url" }
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

    fun isConnected(): Boolean {
        return connectionState.value is ConnectionState.Connected
    }
}