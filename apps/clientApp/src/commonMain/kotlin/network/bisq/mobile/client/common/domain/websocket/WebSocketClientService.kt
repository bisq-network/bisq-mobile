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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientSettings
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.exception.MaximumRetryReachedException
import network.bisq.mobile.client.common.domain.websocket.exception.WebSocketIsReconnecting
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.awaitOrCancel
import network.bisq.mobile.domain.utils.createUuid
import kotlin.concurrent.Volatile

internal data class SubscriptionType(
    val topic: Topic,
    val parameter: String?,
)

/**
 * Listens to httpclient service client changes and creates a new websocket client accordingly
 *
 * Manages websocket subscriptions and resubscribes to events when new websocket clients are instantiated
 */
class WebSocketClientService(
    private val defaultHost: String,
    private val defaultPort: Int,
    private val httpClientService: HttpClientService,
    private val webSocketClientFactory: WebSocketClientFactory,
    private val sessionService: SessionService? = null,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository? = null,
) : ServiceFacade(),
    Logging {
    companion object {
        private const val SESSION_RENEWAL_COOLDOWN_MS = 30_000L

        // Initial subscriptions tracked for network banner:
        private val initialSubscriptionTypes =
            setOf(
                SubscriptionType(Topic.MARKET_PRICE, null),
                SubscriptionType(Topic.NUM_USER_PROFILES, null),
                SubscriptionType(Topic.NUM_OFFERS, null),
            )
    }

    @Volatile
    private var lastSessionRenewalAttemptMs = 0L

    private val clientUpdateMutex = Mutex()
    private val _connectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState = _connectionState.asStateFlow()

    private var stateCollectionJob: Job? = null
    private var currentClientSettings: HttpClientSettings? = null

    private var currentClient = MutableStateFlow<WebSocketClient?>(null)
    private val subscriptionMutex = Mutex()
    private val requestedSubscriptions =
        MutableStateFlow<Map<SubscriptionType, WebSocketEventObserver>>(
            LinkedHashMap(),
        )
    private var subscriptionsAreApplied = false

    private val stopFlow =
        MutableSharedFlow<Unit>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        ) // signal to cancel waiters

    @OptIn(ExperimentalCoroutinesApi::class)
    val initialSubscriptionsReceivedData: Flow<Boolean> =
        requestedSubscriptions.flatMapLatest { subsMap ->
            // Only the first seven subscriptions contribute to the initial data banner
            val trackedObservers =
                initialSubscriptionTypes.mapNotNull { subsMap[it] }
            if (trackedObservers.size < initialSubscriptionTypes.size) {
                flowOf(false)
            } else {
                val hasReceivedDataFlows =
                    trackedObservers.map { it.hasReceivedData }
                combine(hasReceivedDataFlows) { hasReceivedDataArray ->
                    hasReceivedDataArray.all { hasReceivedData -> hasReceivedData }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun activate() {
        super.activate()

        stopFlow.resetReplayCache()

        serviceScope.launch {
            httpClientService.httpClientChangedFlow.collect {
                updateWebSocketClient(it)
            }
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
            currentClientSettings = null
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
            // Skip replacement if current client uses identical settings —
            // avoids disposing a working connection during startup when
            // httpClientChangedFlow emits duplicate/equivalent configs
            // (e.g., from Tor state transitions).
            if (currentClient.value != null && httpClientSettings == currentClientSettings) {
                log.d { "WebSocket client settings unchanged, skipping update" }
                return@withLock
            }

            val newApiUrl: Url =
                httpClientSettings.bisqApiUrl?.takeIf { it.isNotBlank() }?.let {
                    parseUrl(it)
                } ?: parseUrl("http://$defaultHost:$defaultPort")!!

            currentClient.value =
                currentClient.value?.let {
                    log.d { "trusted node changing from ${it.apiUrl} to $newApiUrl. proxy url: ${httpClientSettings.externalProxyUrl}" }
                    it.dispose()
                    currentClientSettings = null
                    null
                }

            // Immediately reflect disconnected state so any code checking
            // isConnected() during the client transition sees the correct state
            // (prevents stale Connected from the disposed client).
            _connectionState.value = ConnectionState.Disconnected()

            // Don't create the WebSocket client until we have valid session credentials.
            // During the pairing flow, settings are first updated with URL/TLS (credentials null),
            // then again with credentials after the pairing HTTP POST succeeds.
            // Connecting without credentials causes 401 on servers with password auth enabled.
            if (httpClientSettings.sessionId.isNullOrBlank() || httpClientSettings.clientId.isNullOrBlank()) {
                log.d { "Skipping WebSocket client creation — session credentials not yet available" }
                stateCollectionJob?.cancel()
                stateCollectionJob = null
                currentClientSettings = null
                _connectionState.value = ConnectionState.Disconnected()
                return@withLock
            }

            val newClient =
                webSocketClientFactory.createNewClient(
                    httpClient = httpClientService.getClient(),
                    apiUrl = newApiUrl,
                    sessionId = httpClientSettings.sessionId,
                    clientId = httpClientSettings.clientId,
                )

            currentClient.value = newClient
            currentClientSettings = httpClientSettings
            ApplicationBootstrapFacade.isDemo = newClient is WebSocketClientDemo
            stateCollectionJob?.cancel()
            stateCollectionJob =
                serviceScope.launch {
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
                                if (state.error is UnauthorizedApiAccessException) {
                                    // Session expired — renew and reconnect with fresh credentials
                                    serviceScope.launch { attemptSessionRenewal() }
                                } else if (shouldAttemptReconnect(state.error)) {
                                    // We disconnected abnormally and we have not reached maximum retry
                                    newClient.reconnect()
                                }
                            }
                        } else if (state is ConnectionState.Connected) {
                            try {
                                applySubscriptions(newClient)
                            } catch (e: Exception) {
                                log.e(e) { "Failed to apply subscriptions after reconnection" }
                            }
                        }
                    }
                }
            log.d { "WebSocket client updated with url $newApiUrl" }

            // Proactively connect the new client so pending requests
            // (e.g. getSettings() during splash navigation) aren't left
            // waiting for an idle disconnected client.
            serviceScope.launch {
                val timeout = WebSocketClient.determineTimeout(newApiUrl.host)
                newClient.connect(timeout)
            }
        }
    }

    private fun shouldAttemptReconnect(error: Throwable): Boolean {
        return when (error) {
            is UnauthorizedApiAccessException,
            is MaximumRetryReachedException,
            is WebSocketIsReconnecting,
            -> false

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

    fun isConnected(): Boolean = connectionState.value is ConnectionState.Connected

    private suspend fun getWsClient(): WebSocketClient =
        awaitOrCancel(
            currentClient.filterNotNull(),
            stopFlow,
        )

    suspend fun subscribe(
        topic: Topic,
        parameter: String? = null,
    ): WebSocketEventObserver {
        // we collect subscriptions here and subscribe to them on a best effort basis
        // if client is not connected yet, it will be accumulated and then subscribed at
        // Connected status, otherwise it will be immediately subscribed
        val (socketObserver, applyNow) =
            subscriptionMutex.withLock {
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

    /**
     * Triggers a reconnection attempt on the current client.
     * Used by [ClientConnectivityService] to recover from max-retry exhaustion
     * when network connectivity returns.
     *
     * Acquires [clientUpdateMutex] to prevent TOCTOU race with [updateWebSocketClient]
     * that could swap/dispose the client between the null-check and reconnect call.
     */
    suspend fun triggerReconnect() {
        clientUpdateMutex.withLock {
            val client = currentClient.value ?: return@withLock
            if (!isConnected()) {
                client.reconnect()
            }
        }
    }

    /**
     * Forces a reconnection regardless of current connection state.
     * Used by [ClientConnectivityService] when a health check fails on a
     * connection that still reports as connected (stale TCP on iOS).
     */
    internal suspend fun forceReconnect() {
        clientUpdateMutex.withLock {
            val client = currentClient.value ?: return@withLock
            client.reconnect()
        }
    }

    /**
     * Sends a lightweight request (settings/version) to verify the connection
     * is actually alive and the server is responsive.
     *
     * @return true if a response was received, false otherwise.
     */
    internal suspend fun sendHealthCheck(): Boolean {
        val client = currentClient.value ?: return false
        val request =
            WebSocketRestApiRequest(
                requestId = createUuid(),
                method = "GET",
                path = "/api/v1/settings/version",
                body = "",
            )
        return try {
            val response = client.sendRequestAndAwaitResponse(request, awaitConnection = false)
            response != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun attemptSessionRenewal() {
        val sessionSvc = sessionService ?: return
        val settingsRepo = sensitiveSettingsRepository ?: return

        val now = DateUtils.now()
        if (now - lastSessionRenewalAttemptMs < SESSION_RENEWAL_COOLDOWN_MS) {
            log.d { "Session renewal on cooldown, skipping" }
            return
        }
        lastSessionRenewalAttemptMs = now

        try {
            val settings = settingsRepo.fetch()
            val clientId = settings.clientId
            val clientSecret = settings.clientSecret
            if (clientId == null || clientSecret == null) {
                log.w { "Cannot renew session — missing clientId or clientSecret" }
                return
            }

            log.i { "Attempting session renewal after 401..." }
            val result = sessionSvc.requestSession(clientId, clientSecret)
            if (result.isSuccess) {
                val response = result.getOrThrow()
                log.i { "Session renewal succeeded, updating settings with new sessionId" }
                settingsRepo.update { it.copy(sessionId = response.sessionId) }
                // Note: settingsRepo.update triggers httpClientChangedFlow → updateWebSocketClient()
                // which creates a new WS client with fresh credentials and connects automatically.
                // No explicit connect() call needed here - it's handled reactively.
            } else {
                log.w { "Session renewal failed: ${result.exceptionOrNull()?.message}" }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Session renewal failed with exception" }
        }
    }

    suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse? = getWsClient().sendRequestAndAwaitResponse(webSocketRequest)

    /**
     * Tests websocket connection to the provided websocket server and proxy
     *
     * @return `null` if the connection test is successful, [Throwable] otherwise.
     */
    suspend fun testConnection(
        apiUrl: Url,
        tlsFingerprint: String? = null,
        clientId: String? = null,
        sessionId: String? = null,
        proxyHost: String? = null,
        proxyPort: Int? = null,
        isTorProxy: Boolean = true,
    ): Throwable? {
        val hasProxy = proxyHost != null && proxyPort != null
        // Explicitly include port in URL to preserve non-default ports (e.g., :80 for HTTP)
        // Ktor's Url.toString() drops default ports, which breaks QR code URLs with explicit ports
        val apiUrlWithPort = "${apiUrl.protocol.name}://${apiUrl.host}:${apiUrl.port}"
        val httpClient =
            httpClientService.createNewInstance(
                HttpClientSettings(
                    bisqApiUrl = apiUrlWithPort,
                    tlsFingerprint = tlsFingerprint,
                    clientId = clientId,
                    sessionId = sessionId,
                    externalProxyUrl = if (hasProxy) "$proxyHost:$proxyPort" else null,
                    isTorProxy = isTorProxy,
                ),
            )
        val wsClient =
            webSocketClientFactory.createNewClient(
                httpClient = httpClient,
                apiUrl = apiUrl,
                clientId = clientId,
                sessionId = sessionId,
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
            wsClient.dispose()
            httpClient.close()
        }
    }
}
