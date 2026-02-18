package network.bisq.mobile.client.common.domain.service.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.utils.Logging
import kotlin.concurrent.Volatile

class ClientConnectivityService(
    private val webSocketClientService: WebSocketClientService,
) : ConnectivityService(),
    Logging {
    companion object {
        const val TIMEOUT = 5000L
        const val PERIOD = 5000L // default check every 5 sec
        const val ROUND_TRIP_SLOW_THRESHOLD = 500L

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

    internal var job: Job? = null
    private val pendingJobs = mutableListOf<Job>()
    private val pendingConnectivityBlocks = mutableListOf<suspend () -> Unit>()
    private val mutex = Mutex()

    /**
     * Starts monitoring connectivity every given period (ms). Default is 10 seconds.
     * @param period of time in ms to check connectivity
     * @param startDelay to delay the first check, default to 5 secs
     */
    fun startMonitoring(
        period: Long = PERIOD,
        startDelay: Long = 5_000,
    ) {
        job?.cancel()
        job =
            serviceScope.launch(Dispatchers.Default) {
                delay(startDelay)
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
//            log.d { "Current average trip time is ${averageTripTime}ms" }
            return averageTripTime > ROUND_TRIP_SLOW_THRESHOLD
        }
        return false // assume is not slow on non mature connections
    }

    private suspend fun checkConnectivity() {
        try {
            val previousStatus = _status.value
            val newStatus =
                when {
                    !isConnected() -> {
                        // Trigger reconnection attempt to recover from max-retry
                        // exhaustion or transient network outages
                        webSocketClientService.triggerReconnect()
                        ConnectivityStatus.RECONNECTING
                    }
                    else -> {
                        // Actively verify the connection with a lightweight request.
                        // On iOS the Darwin engine does not reliably detect dead TCP
                        // connections, so isConnected() can return true even when the
                        // server is down. A real round-trip request detects this.
                        val alive = isConnectionAlive()
                        if (!alive) {
                            log.d { "Health check failed, forcing reconnection" }
                            webSocketClientService.forceReconnect()
                            ConnectivityStatus.RECONNECTING
                        } else if (isSlow()) {
                            ConnectivityStatus.REQUESTING_INVENTORY
                        } else {
                            ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
                        }
                    }
                }
            _status.value = newStatus
            if (previousStatus != newStatus) {
                log.d { "Connectivity transition from $previousStatus to $newStatus" }
                if (previousStatus == ConnectivityStatus.RECONNECTING) {
                    runPendingBlocks()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Failed checking connectivity" }
            _status.value = ConnectivityStatus.DISCONNECTED
        }
    }

    /**
     * Sends a lightweight health check request to verify the server is responsive.
     * Returns true if a response is received within [TIMEOUT], false otherwise.
     */
    private suspend fun isConnectionAlive(): Boolean =
        try {
            withTimeout(TIMEOUT) {
                webSocketClientService.sendHealthCheck()
            }
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

    private fun isConnected(): Boolean = webSocketClientService.isConnected()
}
