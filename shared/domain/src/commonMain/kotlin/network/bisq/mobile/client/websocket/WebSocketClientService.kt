package network.bisq.mobile.client.websocket

import io.ktor.http.Url
import io.ktor.http.parseUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import network.bisq.mobile.client.httpclient.HttpClientService
import network.bisq.mobile.client.httpclient.HttpClientSettings
import network.bisq.mobile.client.httpclient.exception.PasswordIncorrectOrMissingException
import network.bisq.mobile.client.websocket.exception.MaximumRetryReachedException
import network.bisq.mobile.client.websocket.exception.WebSocketIsReconnecting
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging

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
        const val CLEARNET_CONNECT_TIMEOUT = 15_000L
        const val TOR_CONNECT_TIMEOUT = 30_000L
    }

    private val clientUpdateMutex = Mutex()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState = _connectionState.asStateFlow()

    private var stateCollectionJob: Job? = null

    private var currentClient = MutableStateFlow<WebSocketClient?>(null)
    private val subscriptionMutex = Mutex()
    private val requestedSubscriptions = mutableMapOf<SubscriptionType, WebSocketEventObserver>()
    private var subscriptionsAreApplied = false

    override fun activate() {
        super.activate()

        collectIO(httpClientService.httpClientChangedFlow) {
            updateWebSocketClient(it)
        }
    }

    override fun deactivate() {
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
            is CancellationException,
            is WebSocketIsReconnecting -> false
            else ->  {
                error.message?.contains("refused", ignoreCase = true) == true
            }
        }
    }

    suspend fun connect(): Throwable? {
        val client = getWsClient()
        val timeout = determineTimeout(client.apiUrl.host)
        return client.connect(timeout)
    }

    fun isConnected(): Boolean {
        return connectionState.value is ConnectionState.Connected
    }

    private suspend fun getWsClient(): WebSocketClient {
        return withContext(serviceScope.coroutineContext) {
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
            socketObserver.resetSequence()
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

    fun determineTimeout(host: String): Long {
        return if (host.endsWith(".onion")) {
            TOR_CONNECT_TIMEOUT
        } else {
            CLEARNET_CONNECT_TIMEOUT
        }
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
            val timeout = determineTimeout(apiUrl.host)
            val error = wsClient.connect(timeout)
            if (error == null) {
                // Wait 500ms to ensure connection is stable
                kotlinx.coroutines.delay(500)
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
