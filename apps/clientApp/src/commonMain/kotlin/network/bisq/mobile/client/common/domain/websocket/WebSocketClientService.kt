package network.bisq.mobile.client.common.domain.websocket

import io.ktor.http.Url
import io.ktor.http.parseUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientSettings
import network.bisq.mobile.client.common.domain.httpclient.exception.PasswordIncorrectOrMissingException
import network.bisq.mobile.client.common.domain.websocket.exception.MaximumRetryReachedException
import network.bisq.mobile.client.common.domain.websocket.exception.WebSocketIsReconnecting
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.awaitOrCancel

private data class SubscriptionType(val topic: Topic, val parameter: String?)

/**
 * Listens to httpclient service client changes and creates a new websocket client accordingly
 *
 * Manages websocket subscriptions and resubscribes to events when new websocket clients are instantiated
 */
class WebSocketClientService(
    private val defaultHost: String,
    private val defaultPort: Int,
    private val httpClientService: HttpClientService,
    private val webSocketClientFactory: WebSocketClientFactory
) : ServiceFacade(), Logging {

    companion object {
        // Initial subscriptions tracked for network banner:
        private val initialSubscriptionTypes = setOf(
            SubscriptionType(Topic.MARKET_PRICE, null),
            SubscriptionType(Topic.NUM_USER_PROFILES, null),
            SubscriptionType(Topic.NUM_OFFERS, null),
        )
    }

    private val clientUpdateMutex = Mutex()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState = _connectionState.asStateFlow()

    private var stateCollectionJob: Job? = null

    private var currentClient = MutableStateFlow<WebSocketClient?>(null)
    private val subscriptionMutex = Mutex()
    private val requestedSubscriptions = MutableStateFlow<Map<SubscriptionType, WebSocketEventObserver>>(
        LinkedHashMap()
    )
    private var subscriptionsAreApplied = false

    private val stopFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) // signal to cancel waiters

    @OptIn(ExperimentalCoroutinesApi::class)
    val initialSubscriptionsReceivedData: Flow<Boolean> =
        requestedSubscriptions.flatMapLatest { subsMap ->
            // Only the first seven subscriptions contribute to the initial data banner
            val trackedObservers = initialSubscriptionTypes.mapNotNull { subsMap[it] }
            if (trackedObservers.size < initialSubscriptionTypes.size) {
                flowOf(false)
            } else {
                val hasReceivedDataFlows = trackedObservers.map { it.hasReceivedData }
                combine(hasReceivedDataFlows) { hasReceivedDataArray ->
                    hasReceivedDataArray.all { hasReceivedData -> hasReceivedData }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun activate() {
        super.activate()

        stopFlow.resetReplayCache()

        collectIO(httpClientService.httpClientChangedFlow) {
            updateWebSocketClient(it)
        }
    }

    override suspend fun deactivate() {
        stopFlow.tryEmit(Unit)
        super.deactivate()
    }

    /**
     * Disposes the underlying websocket client and the http client used by service.
     * This can be used before a connect call to await instantiation of client due to settings change.
     */
    suspend fun disposeClient() {
        clientUpdateMutex.withLock {
            httpClientService.disposeClient()
            currentClient.value?.dispose()
            currentClient.value = null
            requestedSubscriptions.value.forEach { entry ->
                entry.value.resetSequence()
            }
        }
    }

    /**
     * Initialize the client with settings if available otherwise use defaults
     */
    private suspend fun updateWebSocketClient(httpClientSettings: HttpClientSettings) {
        clientUpdateMutex.withLock {
            val newApiUrl: Url = httpClientSettings.apiUrl?.takeIf { it.isNotBlank() }?.let {
                parseUrl(it)
            } ?: parseUrl("http://$defaultHost:$defaultPort")!!


            currentClient.value = currentClient.value?.let {
                log.d { "trusted node changing from ${it.apiUrl} to $newApiUrl. proxy url: ${httpClientSettings.proxyUrl}" }
                it.dispose()
                null
            }
            val newClient = webSocketClientFactory.createNewClient(
                httpClientService.getClient(),
                newApiUrl,
                httpClientSettings.password,
            )
            currentClient.value = newClient
            ApplicationBootstrapFacade.isDemo = newClient is WebSocketClientDemo
            stateCollectionJob?.cancel()
            stateCollectionJob = launchIO {
                newClient.webSocketClientStatus.collect { state ->
                    _connectionState.value = state
                    if (state is ConnectionState.Disconnected) {
                        subscriptionMutex.withLock {
                            // connection is lost, we need to apply subscriptions again
                            subscriptionsAreApplied = false
                            requestedSubscriptions.value.forEach { entry ->
                                entry.value.resetSequence()
                            }
                        }
                        if (state.error != null) {
                            if (shouldAttemptReconnect(state.error)) {
                                // We disconnected abnormally and we have not reached maximum retry
                                newClient.reconnect()
                            }
                        }
                    } else if (state is ConnectionState.Connected) {
                        applySubscriptions(newClient)
                    }
                }
            }
            log.d { "WebSocket client updated with url $newApiUrl" }
        }
    }

    private fun shouldAttemptReconnect(error: Throwable): Boolean {
        return when (error) {
            is PasswordIncorrectOrMissingException,
            is MaximumRetryReachedException,
            is WebSocketIsReconnecting -> false

            is CancellationException -> {
                if (getPlatformInfo().type == PlatformType.IOS) {
                    return error.cause?.message?.contains("Socket is not connected") == true
                }
                return false
            }

            else -> {
                // we dont want to retry if message contains "refused"
                error.message?.contains("refused", ignoreCase = true) != true
            }
        }
    }

    suspend fun connect(): Throwable? {
        val client = getWsClient()
        val timeout = WebSocketClient.determineTimeout(client.apiUrl.host)
        return client.connect(timeout)
    }

    fun isConnected(): Boolean {
        return connectionState.value is ConnectionState.Connected
    }

    private suspend fun getWsClient(): WebSocketClient {
        return awaitOrCancel(
            currentClient.filterNotNull(),
            stopFlow,
        )
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
            var observer = requestedSubscriptions.value[type]
            if (observer == null) {
                observer = WebSocketEventObserver()
                requestedSubscriptions.update { current ->
                    LinkedHashMap(current).apply { put(type, observer) }
                }
            }
            observer to subscriptionsAreApplied
        }
        if (applyNow) {
            val client = getWsClient()
            log.d { "subscriptions already applied; subscribing to $topic individually" }
            socketObserver.resetSequence()
            client.subscribe(topic, parameter, socketObserver)
        }
        return socketObserver
    }

    private suspend fun applySubscriptions(client: WebSocketClient) {
        subscriptionMutex.withLock {
            if (subscriptionsAreApplied) {
                log.d { "skipping applySubscriptions as we already have subscribed our list" }
                return@withLock
            }
            val subs = requestedSubscriptions.value
            log.d { "applying subscriptions on WS client, entry count: ${subs.size}" }
            subs.forEach { entry ->
                entry.value.resetSequence()
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

    /**
     * Tests websocket connection to the provided websocket server and proxy
     *
     * @return `null` if the connection test is successful, [Throwable] otherwise.
     */
    suspend fun testConnection(
        apiUrl: Url,
        proxyHost: String? = null,
        proxyPort: Int? = null,
        isTorProxy: Boolean = true,
        password: String? = null,
    ): Throwable? {
        val hasProxy = proxyHost != null && proxyPort != null
        val httpClient = httpClientService.createNewInstance(
            HttpClientSettings(
                apiUrl = apiUrl.toString(),
                proxyUrl = if (hasProxy) "$proxyHost:$proxyPort" else null,
                isTorProxy = isTorProxy,
                password = password,
            )
        )
        val wsClient = webSocketClientFactory.createNewClient(
            httpClient,
            apiUrl,
            password,
        )
        try {
            val timeout = WebSocketClient.determineTimeout(apiUrl.host)
            val error = wsClient.connect(timeout)
            if (error == null) {
                // Wait 500ms to ensure connection is stable
                delay(500)
            }
            return error
        } finally {
            launchIO {
                wsClient.dispose()
                httpClient.close()
            }
        }
    }
}
