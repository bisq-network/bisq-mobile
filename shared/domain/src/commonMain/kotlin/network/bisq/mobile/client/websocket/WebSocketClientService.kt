package network.bisq.mobile.client.websocket

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.network.HttpClientService
import network.bisq.mobile.client.network.HttpClientSettings
import network.bisq.mobile.client.network.WebSocketClientFactory
import network.bisq.mobile.client.network.exception.MaximumRetryReachedException
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.ServiceFacade

class WebSocketClientService(
    private val settingsRepository: SettingsRepository,
    private val httpClientService: HttpClientService,
    private val webSocketClientFactory: WebSocketClientFactory,
    private val defaultHost: String,
    private val defaultPort: Int,
) : ServiceFacade() {

    companion object {
        const val TEST_TIMEOUT = 10_000L
    }

    private val wsClient = MutableStateFlow<WebSocketClient?>(null)
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> get() = _connectionState.asStateFlow()

    private var connectionStateJob: Job? = null
    private var errorJob: Job? = null
    private val updateMutex = Mutex()

    override fun activate() {
        super.activate()

        collectIO(httpClientService.httpClientChangedFlow) {
            updateWebSocketClient(settingsRepository.fetch())
        }

        // retry connections whenever disconnected
        collectIO(connectionState) { state ->
            if (state is ConnectionState.Disconnected) {
                if (state.error !is MaximumRetryReachedException) {
                    getWsClient().reconnect()
                }
            }
        }
    }

    override fun deactivate() {
        super.deactivate()
    }

    private suspend fun getWsClient(): WebSocketClient {
        return wsClient.filterNotNull().first()
    }

    suspend fun isDemo(): Boolean {
        return getWsClient().isDemo()
    }

    fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }

    /**
     * Initialize the client with settings if available, otherwise use defaults
     */
    private suspend fun updateWebSocketClient(settings: Settings?) {
        updateMutex.withLock {
            val address = settings?.bisqApiUrl?.let {
                AddressVO.from(it)
            } ?: AddressVO(defaultHost, defaultPort)

            val (newHost, newPort) = address

            // disconnect and cancel old client and status collection
            wsClient.value?.let {
                log.d { "trusted node changing from ${it.host}:${it.port} to $newHost:$newPort" }
                it.disconnect()
            }
            connectionStateJob?.cancel()
            errorJob?.cancel()

            // replace it with new client
            val newWsClient = webSocketClientFactory.createNewClient(
                httpClientService.getClient(),
                newHost,
                newPort
            )
            log.d { "Websocket client initialized with url $newHost:$newPort" }
            wsClient.value = newWsClient
            connectionStateJob =
                collectIO(newWsClient.webSocketClientStatus) { _connectionState.value = it }
        }
    }

    suspend fun connect(): Throwable? {
        return getWsClient().connect()
    }

    suspend fun disconnect() {
        getWsClient().disconnect()
    }

    suspend fun awaitConnection() {
        _connectionState.first { it is ConnectionState.Connected }
    }

    suspend fun subscribe(topic: Topic, parameter: String? = null): WebSocketEventObserver {
        // TODO: track subscriptions and resubscribe on ws client change
        return getWsClient().subscribe(topic, parameter)
    }

    suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse? {
        return getWsClient().sendRequestAndAwaitResponse(webSocketRequest)
    }

    /**
     * Returns null if successful, the error otherwise
     */
    suspend fun testConnection(
        newHost: String,
        newPort: Int,
        newProxyHost: String,
        newProxyPort: Int,
        newUseExternalTorProxy: Boolean,
    ): Throwable? {
        val ws = webSocketClientFactory.createNewClient(
            httpClientService.createNewInstance(
                HttpClientSettings(
                    apiUrl = "$newHost:$newPort",
                    proxyUrl = "$newProxyHost:$newProxyPort",
                    isExternalProxyTor = newUseExternalTorProxy,
                )
            ),
            newHost,
            newPort,
        )

        var error: Throwable? = null
        try {
            withTimeout(TEST_TIMEOUT) {
                error = ws.connect()
            }
        } catch (e: Throwable) {
            error = e
        } finally {
            ws.disconnect()
        }

        return error
    }
}
