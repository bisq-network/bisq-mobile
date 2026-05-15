package network.bisq.mobile.client.common.domain.websocket

import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.parseUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientSettings
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.utils.invalidateUnderlyingSession
import network.bisq.mobile.client.common.domain.websocket.exception.MaximumRetryReachedException
import network.bisq.mobile.client.common.domain.websocket.exception.WebSocketIsReconnecting
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.awaitOrCancel
import network.bisq.mobile.domain.utils.createUuid
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
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

        /**
         * Cooldown between consecutive [forceClientRecreation] invocations triggered
         * by the iOS connect-timeout fast-path. Prevents thrashing if the
         * underlying network (Tor / SOCKS) is itself unhealthy.
         */
        internal const val IOS_CONNECT_TIMEOUT_RECREATION_COOLDOWN_MS = 30_000L

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

    /** Tracks the timestamp of the last iOS-timeout-driven forceClientRecreation. */
    @Volatile
    private var lastIosTimeoutRecreationMs = 0L

    private val _clientRevoked = MutableStateFlow(false)

    /** Emits true when session renewal fails due to revoked credentials (401/403 from server).
     *  Observers should clear stored credentials and navigate the user to the pairing screen. */
    val clientRevoked: StateFlow<Boolean> = _clientRevoked.asStateFlow()

    /** Resets the revocation flag after handling, allowing re-pairing in the same session. */
    fun acknowledgeRevocation() {
        _clientRevoked.value = false
    }

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
    private val _failedSubscriptions = MutableStateFlow<Set<SubscriptionType>>(emptySet())
    val failedSubscriptionTopics: Flow<Set<Topic>> =
        _failedSubscriptions.map { failedSubscriptions ->
            failedSubscriptions.mapTo(LinkedHashSet()) { it.topic }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val isSubscriptionsPending =
        combine(requestedSubscriptions, _failedSubscriptions) { subsMap, failedSubscriptions ->
            subsMap.filterKeys { subscriptionType -> subscriptionType !in failedSubscriptions }
        }.flatMapLatest { pendingSubscriptions ->
            if (pendingSubscriptions.isEmpty()) {
                flowOf(false)
            } else {
                val hasReceivedDataFlows = pendingSubscriptions.values.map { it.hasReceivedData }
                combine(hasReceivedDataFlows) { hasReceivedDataArray ->
                    hasReceivedDataArray.any { hasReceivedData -> !hasReceivedData }
                }
            }
        }

    private val stopFlow =
        MutableSharedFlow<Unit>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        ) // signal to cancel waiters

    @OptIn(ExperimentalCoroutinesApi::class)
    val initialSubscriptionsReceivedData: Flow<Boolean> =
        requestedSubscriptions.flatMapLatest { subsMap ->
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

    private fun clearFailedSubscriptions() {
        _failedSubscriptions.value = emptySet()
    }

    private fun clearSubscriptionFailure(subscriptionType: SubscriptionType) {
        _failedSubscriptions.update { it - subscriptionType }
    }

    private fun markSubscriptionFailed(subscriptionType: SubscriptionType) {
        _failedSubscriptions.update { it + subscriptionType }
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

        // Disconnect the WebSocket and reset subscription state so that
        // a subsequent activate() starts with a clean slate.
        // Without this, subscriptionsAreApplied stays true and the
        // stateCollectionJob (which calls applySubscriptions on connect)
        // is dead — leaving subscriptions registered but uncollected.
        stateCollectionJob?.cancel()
        stateCollectionJob = null
        currentClient.value?.disconnect()
        currentClient.value = null
        currentClientSettings = null
        subscriptionMutex.withLock {
            subscriptionsAreApplied = false
            requestedSubscriptions.value.forEach { it.value.resetSequence() }
            requestedSubscriptions.value = LinkedHashMap()
            clearFailedSubscriptions()
        }
        _connectionState.value = ConnectionState.Disconnected()

        super.deactivate()
    }

    /**
     * Disposes the underlying websocket client and the http client used by service.
     * This can be used before a connect call to await instantiation of client due to settings change.
     */
    suspend fun disposeClient() {
        clientUpdateMutex.withLock {
            // Cancel state collection BEFORE disposing the client so a final dying
            // status emission from the disposed client cannot overwrite the next
            // updateWebSocketClient()'s fresh state. Symmetric with the
            // proxyModeChanged branch in updateWebSocketClient().
            stateCollectionJob?.cancel()
            stateCollectionJob = null
            httpClientService.disposeClient()
            currentClient.value?.dispose()
            currentClient.value = null
            currentClientSettings = null
            _connectionState.value = ConnectionState.Disconnected()
            requestedSubscriptions.value.forEach { entry ->
                entry.value.resetSequence()
            }
            clearFailedSubscriptions()
        }
    }

    /**
     * Initialize the client with settings if available otherwise use defaults
     */
    private suspend fun updateWebSocketClient(httpClientSettings: HttpClientSettings) {
        clientUpdateMutex.withLock {
            val previousSettings = currentClientSettings

            // Skip replacement if current client uses identical settings —
            // avoids disposing a working connection during startup when
            // httpClientChangedFlow emits duplicate/equivalent configs
            // (e.g., from Tor state transitions).
            if (currentClient.value != null && httpClientSettings == previousSettings) {
                log.d { "WebSocket client settings unchanged, skipping update" }
                return@withLock
            }

            // Proxy mode transitions (e.g. Tor → clearnet when switching to demo, or
            // back) must not leak state from the previous client's reconnect loop.
            // Cancel state collection BEFORE disposing so dying status emissions can't
            // overwrite the fresh disconnected state below.
            val proxyModeChanged =
                previousSettings != null && (
                    previousSettings.externalProxyUrl != httpClientSettings.externalProxyUrl ||
                        previousSettings.isTorProxy != httpClientSettings.isTorProxy
                )
            if (proxyModeChanged) {
                log.i {
                    "Proxy mode change: " +
                        "(externalProxyUrl=${previousSettings.externalProxyUrl}, isTor=${previousSettings.isTorProxy}) → " +
                        "(externalProxyUrl=${httpClientSettings.externalProxyUrl}, isTor=${httpClientSettings.isTorProxy})"
                }
                stateCollectionJob?.cancel()
                stateCollectionJob = null
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
                                clearFailedSubscriptions()
                            }
                            if (state.error != null) {
                                if (state.error is UnauthorizedApiAccessException) {
                                    // Session expired — renew and reconnect with fresh credentials
                                    serviceScope.launch { attemptSessionRenewal() }
                                } else if (isIosConnectTimeout(state.error)) {
                                    // iOS connect-timeout leaves a zombie NSURLSessionWebSocketTask
                                    // alive on the underlying NSURLSession for up to 120s(transaction_duration_ms
                                    // Subsequent connect() calls stack additional zombies on the same
                                    // session, poisoning the Tor SOCKS pool until ClientConnectivityService
                                    // fires forceClientRecreation after ~60s (IOS_FORCE_RECREATE_CYCLES=12).
                                    // Trigger forceClientRecreation immediately to drain the zombie now.
                                    val now = DateUtils.now()
                                    val sinceLast = now - lastIosTimeoutRecreationMs
                                    if (sinceLast > IOS_CONNECT_TIMEOUT_RECREATION_COOLDOWN_MS) {
                                        lastIosTimeoutRecreationMs = now
                                        log.e { "iOS WS connect timed out; forcing client recreation early" }
                                        serviceScope.launch { forceClientRecreation() }
                                    } else if (shouldAttemptReconnect(state.error)) {
                                        log.e { "iOS WS connect timeout but recreation cooldown active; falling back to reconnect" }
                                        newClient.reconnect()
                                    }
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

    /**
     * Detects whether [error] represents an iOS-platform connect timeout where the
     * underlying [NSURLSessionWebSocketTask] is likely still alive holding a Tor SOCKS slot.
     *
     * Two cases are handled:
     *
     * 1. [TimeoutCancellationException] / "Timed out waiting": The Darwin engine does not
     *    propagate Kotlin coroutine cancellation to the platform task, so a zombie
     *    NSURLSessionWebSocketTask remains alive after the Kotlin timeout fires.
     *
     * 2. NSURLErrorDomain Code=-1000 "bad URL": The iOS Tor SOCKS proxy accepted the TCP
     *    connection but returned SOCKS general-failure (status 1), meaning the proxy is up
     *    but cannot route to the .onion destination (circuit not yet established). The task
     *    is dead and the SOCKS connection slot is wasted. Periodic [forceClientRecreation]
     *    opens a fresh NSURLSession (new SOCKS handshake), giving the Tor daemon an
     *    opportunity to assign a different — potentially healthier — circuit for the next
     *    connection attempt.
     */
    private fun isIosConnectTimeout(error: Throwable): Boolean {
        if (getPlatformInfo().type != PlatformType.IOS) return false
        if (error is kotlinx.coroutines.TimeoutCancellationException) return true
        val msg = error.message ?: return false
        if (msg.contains("Timed out waiting", ignoreCase = true)) return true
        return msg.contains("bad URL", ignoreCase = true)
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
        val type = SubscriptionType(topic, parameter)
        val (socketObserver, applyNow) =
            subscriptionMutex.withLock {
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
            try {
                client.subscribe(topic, parameter, socketObserver)
                clearSubscriptionFailure(type)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    log.e(e) { "Failed to subscribe to topic $topic; skipping" }
                    markSubscriptionFailed(type)
                }
                currentCoroutineContext().ensureActive()
            }
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
                try {
                    entry.value.resetSequence()
                    client.subscribe(
                        entry.key.topic,
                        entry.key.parameter,
                        entry.value,
                    )
                    clearSubscriptionFailure(entry.key)
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        log.e(e) { "Failed to subscribe to topic ${entry.key.topic}; skipping" }
                        markSubscriptionFailed(entry.key)
                    }
                    currentCoroutineContext().ensureActive()
                }
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
     * Forces full client recreation: disposes the current WebSocket client and
     * re-triggers [updateWebSocketClient] with the same settings, producing a
     * brand-new [HttpClient] and [WebSocketClientImpl].
     *
     * Used on iOS where the Darwin engine's NSURLSession may not create
     * functional WebSocket connections after repeated disconnections on the
     * same session instance.
     */
    internal suspend fun forceClientRecreation() {
        // Update the recreation timestamp here (not only in stateCollectionJob) so that
        // CCS-triggered recreations also reset the cooldown. Without this, a CCS recreation
        // doesn't update lastIosTimeoutRecreationMs, and the next "bad URL" error in
        // stateCollectionJob sees sinceLast > 30s and immediately fires another recreation,
        // causing back-to-back client churn every ~23-30s.
        if (getPlatformInfo().type == PlatformType.IOS) {
            lastIosTimeoutRecreationMs = DateUtils.now()
        }
        clientUpdateMutex.withLock {
            if (currentClientSettings == null) return@withLock
            log.i { "Forcing full client recreation to recover stale iOS NSURLSession" }
            // Cancel state collection before disposing the client so a final status
            // emission from the dying client cannot overwrite fresh state.
            stateCollectionJob?.cancel()
            stateCollectionJob = null

            val oldClient = currentClient.value

            // CRITICAL CRASH FIX: the WebSocketClientImpl's
            // `reconnectJob` and `clientScope` MUST be cancelled BEFORE we
            // invalidate the underlying NSURLSession. Otherwise the in-flight
            // reconnect job's `invokeOnCompletion` receives the
            // invalidate-triggered `DarwinHttpRequestException("…cancelled")`,
            // sees `it == null` (the deferred completed *normally* returning
            // the error), and recursively calls `reconnect()`. That spawns a
            // fresh `connect()` → `httpClient.webSocketSession { … }` →
            // `NSURLSession.webSocketTaskForRequest(…)` on the now-invalidated
            // session, raising the uncatchable
            //     NSGenericException: 'Task created in a session that has been invalidated'
            // which crashes the app.
            //
            // `prepareForRecreation()` cancels both `reconnectJob` and `clientScope`
            // It is non-blocking and does NOT acquire `connectionMutex`, so it cannot itself stall on a
            // doomed `withTimeout(30000) { webSocketSession {…} }`.
            oldClient?.prepareForRecreation()

            // BEFORE we dispose() the WebSocketClient, forcibly invalidate the iOS
            // NSURLSession that backs its HttpClient.
            httpClientService.peekClient()?.invalidateUnderlyingSession()

            // Dispose current client and clear settings so updateWebSocketClient
            // treats the next call as a fresh configuration.
            currentClient.value = null
            oldClient?.dispose()
            subscriptionMutex.withLock {
                subscriptionsAreApplied = false
                requestedSubscriptions.value.forEach { it.value.resetSequence() }
                clearFailedSubscriptions()
            }
            _connectionState.value = ConnectionState.Disconnected()
            currentClientSettings = null
            // Re-trigger with same settings — this creates fresh httpClient + wsClient
            // Must release clientUpdateMutex first since updateWebSocketClient acquires it
        }
        // Call outside the lock since updateWebSocketClient acquires clientUpdateMutex
        httpClientService.recreateClient()
    }

    /**
     * Sends a lightweight request (settings/version) to verify the connection
     * is actually alive and the server is responsive.
     *
     * @return true if a response was received, false otherwise.
     */
    @ExcludeFromCoverage
    internal suspend fun sendHealthCheck(): Boolean {
        val client = currentClient.value ?: return false
        val request =
            WebSocketRestApiRequest(
                requestId = createUuid(),
                method = "GET",
                path = WebSocketClientImpl.HEALTH_CHECK_PATH,
                body = "",
            )
        return try {
            val response = client.sendRequestAndAwaitResponse(request, awaitConnection = false)
            // Detect expired/revoked session: the server responds with 401 (session expired)
            // or 403 (client revoked) inside the WebSocket response. Without this check, the
            // health check reports "alive" even though all API calls will fail.
            if (response is WebSocketRestApiResponse &&
                (
                    response.httpStatusCode == HttpStatusCode.Unauthorized ||
                        response.httpStatusCode == HttpStatusCode.Forbidden
                )
            ) {
                throw UnauthorizedApiAccessException()
            }
            response != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedApiAccessException) {
            throw e // Propagate so the connection state handler triggers session renewal
        } catch (e: Exception) {
            false
        }
    }

    @ExcludeFromCoverage
    internal suspend fun attemptSessionRenewal() {
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
                val error = result.exceptionOrNull()
                if (error is UnauthorizedApiAccessException) {
                    // Server rejected our credentials — client profile was revoked.
                    // Clear stored credentials, dispose stale HTTP/WS clients, and
                    // signal the UI to navigate to re-pairing.
                    log.e { "Client credentials revoked — clearing stored pairing data" }
                    settingsRepo.update {
                        it.copy(clientId = null, clientSecret = null, sessionId = null)
                    }
                    // Dispose the HTTP client so re-pairing creates a fresh one
                    // (the old client has stale TLS settings that cause connection reset)
                    httpClientService.disposeClient()
                    _clientRevoked.value = true
                } else {
                    log.w { "Session renewal failed: ${error?.message}" }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedApiAccessException) {
            // HTTP client validator threw 401 directly (before result wrapping)
            log.e { "Client credentials revoked (exception) — clearing stored pairing data" }
            settingsRepo.update {
                it.copy(clientId = null, clientSecret = null, sessionId = null)
            }
            httpClientService.disposeClient()
            _clientRevoked.value = true
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
            // Symmetric to HttpClientService.disposeClient(): invalidate the underlying
            // NSURLSession on iOS so this ephemeral test client cannot leak a zombie task
            // or pollute the next client's pool via a shared SOCKS connection slot.
            httpClient.invalidateUnderlyingSession()
            httpClient.close()
        }
    }
}
