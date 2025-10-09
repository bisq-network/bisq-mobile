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
import kotlinx.coroutines.withContext
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
import io.ktor.utils.io.CancellationException as KtorCancellationException

private data class SubscriptionType(val topic: Topic, val parameter: String?)

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
    private val clientUpdateMutex = Mutex()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState = _connectionState.asStateFlow()

    private var observeSettingsJob: Job? = null
    private var stateCollectionJob: Job? = null

    private val ioScope = CoroutineScope(IODispatcher)

    private var currentClient = MutableStateFlow<WebSocketClient?>(null)
    private val subscriptionMutex = Mutex()
    private val requestedSubscriptions = mutableMapOf<SubscriptionType, WebSocketEventObserver>()
    private var subscriptionsAreApplied = false

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
        currentClient.value?.dispose()
        currentClient.value = null
        stateCollectionJob?.cancel()
        observeSettingsJob?.cancel()
        observeSettingsJob = ioScope.launch {
            settingsRepository.data.collect {
                updateWebSocketClient(it)
            }
        }

        getWsClient()
    }

    /**
     * Initialize the client with settings if available otherwise use defaults
     */
    private suspend fun updateWebSocketClient(settings: Settings?) {
        clientUpdateMutex.withLock {
            val address = settings?.bisqApiUrl?.takeIf { it.isNotBlank() }?.let {
                AddressVO.from(it)
            } ?: AddressVO(defaultHost, defaultPort)

            val (newHost, newPort) = address


            if (isDifferentFromCurrentClient(newHost, newPort)) {
                currentClient.value = currentClient.value?.let {
                    log.d { "trusted node changing from ${it.host}:${it.port} to $newHost:$newPort" }
                    it.dispose()
                    null
                }
                val newClient = createClient(newHost, newPort)
                currentClient.value = newClient
                ApplicationBootstrapFacade.isDemo = newClient is WebSocketClientDemo
                stateCollectionJob?.cancel()
                stateCollectionJob = ioScope.launch {
                    newClient.webSocketClientStatus.collect { state ->
                        _connectionState.value = state
                        if (state is ConnectionState.Disconnected) {
                            subscriptionMutex.withLock {
                                // connection is lost, we need to apply subscriptions again
                                subscriptionsAreApplied = false
                            }
                            if (state.error != null) {
                                if (state.error !is MaximumRetryReachedException &&
                                    state.error !is CancellationException &&
                                    state.error !is KtorCancellationException &&
                                    state.error !is WebSocketIsReconnecting &&
                                    state.error.message?.contains("refused") != true
                                ) {
                                    // We disconnected abnormally and we have not reached maximum retry
                                    newClient.reconnect()
                                }
                            }
                        } else if (state is ConnectionState.Connected) {
                            applySubscriptions(newClient)
                        }
                    }
                }
                log.d { "WebSocket client updated with url $newHost:$newPort" }
            }
        }
    }

    suspend fun connect(timeout: Long = 10000L): Throwable? {
        return getWsClient().connect(timeout)
    }

    private fun isDifferentFromCurrentClient(host: String, port: Int): Boolean {
        val current = currentClient.value
        return current == null || current.host != host || current.port != port
    }

    fun isConnected(): Boolean {
        return connectionState.value is ConnectionState.Connected
    }

    private suspend fun getWsClient(): WebSocketClient {
        return withContext(ioScope.coroutineContext) {
            currentClient.filterNotNull().first()
        }
    }

    suspend fun subscribe(
        topic: Topic,
        parameter: String? = null,
    ): WebSocketEventObserver {
        // we collect subscriptions here and subscribe to them on a best effort basis
        // if client is not connected yet, it will be accumulated and then subscribed at
        // Connected status, otherwise it will be immediately subscribed
        val (socketObserver, applyNow) = subscriptionMutex.withLock {
            val type = SubscriptionType(topic, parameter)
            val observer = requestedSubscriptions.getOrPut(type) { WebSocketEventObserver() }
            observer to subscriptionsAreApplied
        }
        if (applyNow) {
            val client = getWsClient()
            log.d { "subscriptions already applied; subscribing to $topic individually" }
            client.subscribe(topic, parameter, socketObserver)
        }
        return socketObserver
    }

    private suspend fun applySubscriptions(client: WebSocketClient) {
        subscriptionMutex.withLock {
            if (subscriptionsAreApplied) {
                log.d { "skipping applySubscriptions as we already have subscribed our list" }
            } else {
                log.d { "applying subscriptions on WS client, entry count: ${requestedSubscriptions.size}" }
            }
            requestedSubscriptions.forEach { entry ->
                client.subscribe(
                    entry.key.topic,
                    entry.key.parameter,
                    entry.value,
                )
            }
            subscriptionsAreApplied = true
        }
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