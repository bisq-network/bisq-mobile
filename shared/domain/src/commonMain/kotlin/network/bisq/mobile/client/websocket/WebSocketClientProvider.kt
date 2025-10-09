package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.websocket.exception.MaximumRetryReachedException
import network.bisq.mobile.client.websocket.exception.WebSocketIsReconnecting
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging

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
    private val updateMutex = Mutex()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState = _connectionState.asStateFlow()

    private var observeSettingsJob: Job? = null
    private var stateCollectionJob: Job? = null

    private val ioScope = CoroutineScope(IODispatcher)

    private var currentClient = MutableStateFlow<WebSocketClient?>(null)

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
     * setups the observers and waits till the websocket client is initialized
     */
    suspend fun initialize() {
        currentClient.value?.disconnect()
        currentClient.value = null
        stateCollectionJob?.cancel()
        observeSettingsJob?.cancel()
        observeSettingsJob = ioScope.launch {
            settingsRepository.data.collect {
                updateWebSocketClient(it)
            }
        }

        currentClient.filterNotNull().first()
    }

    /**
     * Initialize the client with settings if available otherwise use defaults
     */
    private suspend fun updateWebSocketClient(settings: Settings?) {
        updateMutex.withLock {
            val address = settings?.bisqApiUrl?.takeIf { it.isNotBlank() }?.let {
                AddressVO.from(it)
            } ?: AddressVO(defaultHost, defaultPort)

            val (newHost, newPort) = address


            if (isDifferentFromCurrentClient(newHost, newPort)) {
                currentClient.value?.let {
                    log.d { "trusted node changing from ${it.host}:${it.port} to $newHost:$newPort" }
                    it.disconnect()
                }
                val newClient = createClient(newHost, newPort)
                currentClient.value = newClient
                ApplicationBootstrapFacade.isDemo = newClient is WebSocketClientDemo
                stateCollectionJob?.cancel()
                stateCollectionJob = ioScope.launch {
                    newClient.webSocketClientStatus.collect { state ->
                        _connectionState.value = state
                        if (state is ConnectionState.Disconnected) {
                            if (state.error != null) {
                                if (state.error !is MaximumRetryReachedException &&
                                    state.error !is CancellationException &&
                                    state.error !is WebSocketIsReconnecting &&
                                    state.error.message?.contains("refused") != true) {
                                    // We disconnected abnormally and we have not reached maximum retry
                                    newClient.reconnect()
                                }
                            }
                        }
                    }
                }
                log.d { "WebSocket client updated with url $newHost:$newPort" }
            }
        }
    }

    suspend fun connect(timeout: Long = 10000L): Throwable? {
        return currentClient.filterNotNull().first().connect(timeout)
    }

    private fun isDifferentFromCurrentClient(host: String, port: Int): Boolean {
        val current = currentClient.value
        return current == null || current.host != host || current.port != port
    }

    fun isConnected(): Boolean {
        return connectionState.value is ConnectionState.Connected
    }

    private suspend fun getWsClient(): WebSocketClient {
        return currentClient.filterNotNull().first()
    }

    suspend fun subscribe(topic: Topic, parameter: String? = null): WebSocketEventObserver {
        // TODO: track subscriptions and resubscribe on ws client change
        return getWsClient().subscribe(topic, parameter)
    }

    suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse? {
        return getWsClient().sendRequestAndAwaitResponse(webSocketRequest)
    }

    // TODO: will be removed with introduction of httpclient service
    /**
     * Suspends till websocket client is not null, then returns with host:port value
     */
    suspend fun getWebSocketHostname(): String {
        val client = getWsClient()
        return "${client.host}:${client.port}"
    }
}