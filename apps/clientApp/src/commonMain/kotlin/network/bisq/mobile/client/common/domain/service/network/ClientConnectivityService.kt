package network.bisq.mobile.client.common.domain.service.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService.Companion.TIMEOUT
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.utils.Logging
import kotlin.concurrent.Volatile

open class ClientConnectivityService(
    private val webSocketClientService: WebSocketClientService,
    private val platformInfo: PlatformInfo = getPlatformInfo(),
) : ConnectivityService(),
    Logging {
    companion object {
        const val TIMEOUT = 5000L

        // Tor health checks need a much larger ceiling than clearnet: .onion round trips
        // routinely take several seconds and the TCP/WS upgrade alone can exceed 5s on a
        // poor circuit. A flat 5s timeout fails healthy-but-slow Tor connections, tearing
        // them down and forcing an expensive reconnect (~7s + re-subscribing every topic),
        // which is exactly the "frequent Reconnecting" loop this avoids.
        const val TIMEOUT_TOR = 15_000L
        const val PERIOD = 5000L // default check every 5 sec
        const val START_DELAY = 5_000L
        const val START_DELAY_TOR = 20_000L
        const val ROUND_TRIP_SLOW_THRESHOLD = 500L

        // Tor .onion round trips are typically 1–3 s, so a 500 ms threshold would
        // permanently keep status at REQUESTING_INVENTORY on Tor connections.
        const val ROUND_TRIP_SLOW_THRESHOLD_TOR = 3000L
        internal const val IOS_FORCE_RECREATE_CYCLES = 12 // ~60s at default 5s period
        internal const val ANDROID_FORCE_RECREATE_CYCLES = 12 // ~60s at default 5s period

        // Consecutive failed health checks tolerated before tearing down a connection
        // that isConnected() still reports as alive. A single slow/missed round trip
        // (common on Tor) must not flip the UI to RECONNECTING.
        internal const val HEALTH_CHECK_FAILURE_THRESHOLD = 2

        private const val DEFAULT_AVERAGE_TRIP_TIME = -1L // invalid
        const val MIN_REQUESTS_TO_ASSESS_SPEED = 3 // invalid

        @Volatile
        private var sessionTotalRequests = 0L

        @Volatile
        private var averageTripTime = DEFAULT_AVERAGE_TRIP_TIME

        fun newRequestRoundTripTime(timeInMs: Long) {
            averageTripTime =
                when (averageTripTime) {
                    DEFAULT_AVERAGE_TRIP_TIME -> {
                        timeInMs
                    }

                    else -> {
                        (averageTripTime + timeInMs) / 2
                    }
                }
            sessionTotalRequests++
        }

        /**
         * Resets the average trip time tracking. Used by tests to ensure clean state.
         */
        internal fun resetAverageTripTime() {
            averageTripTime = DEFAULT_AVERAGE_TRIP_TIME
            sessionTotalRequests = 0
        }
    }

    /**
     * Current average request round-trip time in ms, or a negative sentinel if no API request has
     * completed yet. Only REST-over-WS requests update this (not health checks or subscriptions),
     * so it is a point-in-time snapshot — read it when a screen is shown rather than observing it.
     */
    fun currentAverageRoundTripTimeMs(): Long = averageTripTime

    internal var job: Job? = null
    private val pendingJobs = mutableListOf<Job>()
    private val pendingConnectivityBlocks = mutableListOf<suspend () -> Unit>()
    private val mutex = Mutex()
    private var consecutiveReconnectingCycles = 0

    // Debounce for the trusted→untrusted transition: counts consecutive failed health
    // checks while isConnected() still reports alive. Reset on any successful check.
    private var consecutiveHealthCheckFailures = 0

    // Once a health check fails, the connection is "untrusted" until a health check
    // succeeds again. This prevents the monitoring loop from oscillating between
    // CONNECTED and RECONNECTING when isConnected() returns true on a stale/half-open
    // TCP connection (e.g., desktop node shutdown without sending TCP FIN).
    private var connectionUntrusted = false

    /** Emits true when the server has permanently revoked our client credentials. */
    val clientRevoked: StateFlow<Boolean> get() = webSocketClientService.clientRevoked

    override suspend fun activate() {
        super.activate()
        consecutiveReconnectingCycles = 0
        consecutiveHealthCheckFailures = 0
        connectionUntrusted = false
        startMonitoring()
    }

    override suspend fun deactivate() {
        stopMonitoring()
        consecutiveReconnectingCycles = 0
        consecutiveHealthCheckFailures = 0
        connectionUntrusted = false
        super.deactivate()
    }

    internal fun monitoringStartDelay(): Long = if (webSocketClientService.isTorProxy) START_DELAY_TOR else START_DELAY

    /**
     * Starts monitoring connectivity every given period (ms). Default is 5 seconds ([PERIOD]).
     * @param period of time in ms to check connectivity
     * @param startDelay explicit delay before the first check; when null (default) a two-phase
     *   delay is used: always wait [START_DELAY] first (by which time proxy settings are
     *   guaranteed to be loaded), then read [isTorProxy] and add the remaining Tor delay if
     *   needed.  This avoids a startup race where [monitoringStartDelay] reads `isTorProxy`
     *   before [WebSocketClientService.currentClientSettings] has been applied.
     */
    fun startMonitoring(
        period: Long = PERIOD,
        startDelay: Long? = null,
    ) {
        job?.cancel()
        job =
            serviceScope.launch(Dispatchers.Default) {
                if (startDelay != null) {
                    delay(startDelay)
                } else {
                    // Phase 1: base delay — always wait at least the non-Tor start delay.
                    // By the time this elapses, WebSocketClientService.currentClientSettings
                    // is guaranteed to have been applied, so isTorProxy is reliable.
                    delay(START_DELAY)
                    // Phase 2: if the connection uses Tor, wait the additional time.
                    if (webSocketClientService.isTorProxy) {
                        delay(START_DELAY_TOR - START_DELAY)
                    }
                }
                while (true) {
                    checkConnectivity()
                    delay(period)
                }
            }
        log.d { "Connectivity is being monitored" }
    }

    /**
     * Default implementation uses round trip average measuring.
     * It relays on other components updating it on each request.
     */
    @Throws(IllegalStateException::class)
    protected suspend fun isSlow(): Boolean {
        if (sessionTotalRequests > MIN_REQUESTS_TO_ASSESS_SPEED) {
            val threshold =
                if (webSocketClientService.isTorProxy) ROUND_TRIP_SLOW_THRESHOLD_TOR else ROUND_TRIP_SLOW_THRESHOLD
            return averageTripTime > threshold
        }
        return false // assume is not slow on non mature connections
    }

    private suspend fun checkConnectivity() {
        val previousStatus = status.value
        try {
            val connected = isConnected()
            log.d { "Health cycle: isConnected=$connected, connectionUntrusted=$connectionUntrusted, previousStatus=$previousStatus" }
            val rawStatus =
                when {
                    !connected || connectionUntrusted -> {
                        // When connectionUntrusted is true, we don't trust isConnected()
                        // alone — a previous health check failed, meaning the server may
                        // be down even though the TCP socket looks alive (half-open connection).
                        // We must verify with a real round-trip before trusting the connection.
                        if (!connected) {
                            // A dropped connection ends the current trusted session, so restart
                            // the health-check grace counter — the next reconnected session must
                            // get a fresh single-miss tolerance rather than inheriting a stale count.
                            consecutiveHealthCheckFailures = 0
                            consecutiveReconnectingCycles++
                            log.d { "Not connected, consecutiveReconnectingCycles=$consecutiveReconnectingCycles" }
                            if (shouldForceClientRecreation()) {
                                log.i { "Forcing client recreation after $consecutiveReconnectingCycles failed cycles (platform=${platformInfo.type})" }
                                webSocketClientService.forceClientRecreation()
                                consecutiveReconnectingCycles = 0
                            } else {
                                webSocketClientService.triggerReconnect()
                            }
                            ConnectivityStatus.RECONNECTING
                        } else {
                            // isConnected() is true but connection is untrusted.
                            // Verify with a health check before restoring trust.
                            val alive = isConnectionAlive()
                            log.d { "Untrusted connection health check: alive=$alive" }
                            if (alive) {
                                log.i { "Connection trust restored after successful health check" }
                                connectionUntrusted = false
                                consecutiveReconnectingCycles = 0
                                consecutiveHealthCheckFailures = 0
                                connectedStatus()
                            } else {
                                consecutiveReconnectingCycles++
                                log.d { "Untrusted connection health check failed, consecutiveReconnectingCycles=$consecutiveReconnectingCycles" }
                                if (shouldForceClientRecreation()) {
                                    log.i { "Forcing client recreation after $consecutiveReconnectingCycles failed untrusted cycles (platform=${platformInfo.type})" }
                                    webSocketClientService.forceClientRecreation()
                                    consecutiveReconnectingCycles = 0
                                } else {
                                    webSocketClientService.forceReconnect()
                                }
                                ConnectivityStatus.RECONNECTING
                            }
                        }
                    }
                    else -> {
                        consecutiveReconnectingCycles = 0
                        // Actively verify the connection with a lightweight request.
                        // On iOS the Darwin engine does not reliably detect dead TCP
                        // connections, so isConnected() can return true even when the
                        // server is down. A real round-trip request detects this.
                        val alive = isConnectionAlive()
                        log.d { "Health check result: alive=$alive" }
                        if (alive) {
                            consecutiveHealthCheckFailures = 0
                            connectedStatus()
                        } else {
                            consecutiveHealthCheckFailures++
                            if (consecutiveHealthCheckFailures < HEALTH_CHECK_FAILURE_THRESHOLD) {
                                // Tolerate a transient slow/missed health check without tearing
                                // down a connection that still looks alive. Re-verify next cycle;
                                // only commit to RECONNECTING after HEALTH_CHECK_FAILURE_THRESHOLD
                                // consecutive misses. Avoids flapping to RECONNECTING on Tor.
                                log.d {
                                    "Health check missed " +
                                        "($consecutiveHealthCheckFailures/$HEALTH_CHECK_FAILURE_THRESHOLD), tolerating"
                                }
                                connectedStatus()
                            } else {
                                log.d { "Health check failed $consecutiveHealthCheckFailures consecutive times, marking connection as untrusted" }
                                connectionUntrusted = true
                                consecutiveHealthCheckFailures = 0
                                webSocketClientService.forceReconnect()
                                ConnectivityStatus.RECONNECTING
                            }
                        }
                    }
                }
            setConnectivityStatus(rawStatus)
            val newStatus = status.value
            if (previousStatus != newStatus) {
                log.d { "Connectivity transition from $previousStatus to $newStatus" }
                if (!previousStatus.isConnected() && newStatus.isConnected()) {
                    runPendingBlocks()
                }
            }
        } catch (e: UnauthorizedApiAccessException) {
            // Session expired — the WebSocket is alive but the server rejects our session.
            // Renew the session first (gets new sessionId), which triggers httpClientChangedFlow
            // → updateWebSocketClient() → fresh WebSocket with valid credentials.
            // We must NOT call forceReconnect() here because that reconnects with the stale
            // sessionId, which the server immediately rejects.
            log.i { "Session expired (401 from health check), attempting session renewal" }
            setConnectivityStatus(ConnectivityStatus.RECONNECTING)
            connectionUntrusted = true
            try {
                webSocketClientService.attemptSessionRenewal()
            } catch (renewalError: Exception) {
                log.e(renewalError) { "Session renewal failed" }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Failed checking connectivity" }
            setConnectivityStatus(ConnectivityStatus.DISCONNECTED)
        }
    }

    private fun shouldForceClientRecreation(): Boolean =
        consecutiveReconnectingCycles >=
            when (platformInfo.type) {
                PlatformType.IOS -> IOS_FORCE_RECREATE_CYCLES
                PlatformType.ANDROID -> ANDROID_FORCE_RECREATE_CYCLES
            }

    /**
     * The connected status to report when a health check has just succeeded: degraded if
     * some subscriptions failed, still-loading if the link is slow, otherwise fully connected.
     */
    private suspend fun connectedStatus(): ConnectivityStatus {
        val failedSubs = webSocketClientService.failedSubscriptionTopics.first()
        return when {
            failedSubs.isNotEmpty() -> ConnectivityStatus.CONNECTED_WITH_LIMITATIONS
            isSlow() -> ConnectivityStatus.REQUESTING_INVENTORY
            else -> ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
        }
    }

    /** Health-check timeout, larger for Tor where round trips and the WS upgrade are slow. */
    internal fun healthCheckTimeout(): Long = if (webSocketClientService.isTorProxy) TIMEOUT_TOR else TIMEOUT

    /**
     * Sends a lightweight health check request to verify the server is responsive.
     * Returns true if a response is received within [healthCheckTimeout], false otherwise.
     * @throws UnauthorizedApiAccessException if the session has expired (401 response)
     */
    private suspend fun isConnectionAlive(): Boolean =
        try {
            withTimeout(healthCheckTimeout()) {
                webSocketClientService.sendHealthCheck()
            }
        } catch (e: UnauthorizedApiAccessException) {
            // Session expired — propagate so checkConnectivity can trigger session renewal
            throw e
        } catch (e: TimeoutCancellationException) {
            log.d { "Health check timed out" }
            false
        } catch (e: CancellationException) {
            // If the monitoring coroutine itself is cancelled (stopMonitoring),
            // propagate so the loop stops. Otherwise the cancellation came from
            // the WebSocket layer (e.g. OkHttp detecting a dead connection and
            // disposing the in-flight request) — treat as health check failure.
            currentCoroutineContext().ensureActive()
            log.d { "Health check cancelled by WebSocket disconnect" }
            false
        } catch (e: Exception) {
            log.d { "Health check failed: ${e.message}" }
            false
        }

    private fun runPendingBlocks() {
        serviceScope.launch(Dispatchers.Default) {
            mutex.withLock {
                val blocksToExecute =
                    pendingConnectivityBlocks.let {
                        val blocks = it.toList()
                        pendingConnectivityBlocks.clear()
                        blocks
                    }

                if (blocksToExecute.isNotEmpty()) {
                    log.d { "Executing ${blocksToExecute.size} pending connectivity blocks" }

                    blocksToExecute.forEach { block ->
                        // Use service scope intentionally to avoid cancellation
                        val job =
                            serviceScope.launch {
                                try {
                                    block()
                                } catch (e: Exception) {
                                    log.e(e) { "Error executing pending connectivity block" }
                                } finally {
                                    pendingJobs.remove(this.coroutineContext[Job])
                                }
                            }
                        pendingJobs.add(job)
                    }
                }
            }
        }
    }

    fun stopMonitoring() {
        job?.cancel()
        job = null
        pendingJobs.forEach { it.cancel() }
        pendingJobs.clear()
        // Clear any pending blocks to prevent memory leaks
        serviceScope.launch {
            mutex.withLock {
                pendingConnectivityBlocks.clear()
                log.d { "Cleared pending connectivity blocks" }
            }
        }
        log.d { "Connectivity stopped being monitored" }
    }

    /** Resets the revocation flag after handling (e.g., after navigating to pairing screen). */
    fun acknowledgeRevocation() {
        webSocketClientService.acknowledgeRevocation()
    }

    private fun isConnected(): Boolean = webSocketClientService.isConnected()
}
