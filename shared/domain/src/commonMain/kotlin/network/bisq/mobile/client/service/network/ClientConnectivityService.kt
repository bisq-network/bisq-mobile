package network.bisq.mobile.client.service.network

import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.utils.Logging
import kotlin.concurrent.Volatile

class ClientConnectivityService(
    private val webSocketClientProvider: WebSocketClientProvider
) : ConnectivityService(), Logging {

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
            averageTripTime = when (averageTripTime) {
                DEFAULT_AVERAGE_TRIP_TIME -> {
                    timeInMs
                }

                else -> {
                    (averageTripTime + timeInMs) / 2
                }
            }
            sessionTotalRequests++
        }
    }

    private var job: Job? = null
    private val pendingJobs = mutableListOf<Job>()
    private val pendingConnectivityBlocks = mutableListOf<suspend () -> Unit>()
    private val mutex = Mutex()


    /**
     * Starts monitoring connectivity every given period (ms). Default is 10 seconds.
     * @param period of time in ms to check connectivity
     * @param startDelay to delay the first check, default to 5 secs
     */
    fun startMonitoring(period: Long = PERIOD, startDelay: Long = 5_000) {
        job?.cancel()
        job = launchIO {
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
            withTimeout(TIMEOUT) {
                val previousStatus = _status.value
                _status.value = when {
                    !isConnected() -> ConnectivityStatus.RECONNECTING
                    isSlow() -> ConnectivityStatus.REQUESTING_INVENTORY
                    else -> ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
                }
                if (previousStatus != _status.value) {
                    log.d { "Connectivity transition from $previousStatus to ${_status.value}" }
                    if (previousStatus == ConnectivityStatus.RECONNECTING) {
                        runPendingBlocks()
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            log.e(e) { "Connectivity check timed out, assuming no connection" }
            _status.value = ConnectivityStatus.DISCONNECTED
        } catch (e: Exception) {
            log.e(e) { "Failed checking connectivity, assuming no connection" }
            _status.value = ConnectivityStatus.DISCONNECTED
        }
    }

    private fun runPendingBlocks() {
        launchIO {
            mutex.withLock {
                val blocksToExecute = pendingConnectivityBlocks.let {
                    val blocks = it.toList()
                    pendingConnectivityBlocks.clear()
                    blocks
                }

                if (blocksToExecute.isNotEmpty()) {
                    log.d { "Executing ${blocksToExecute.size} pending connectivity blocks" }

                    blocksToExecute.forEach { block ->
                        // Use service scope intentionally to avoid cancellation
                        val job = serviceScope.launch {
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

    /**
     * Executes the given block when connectivity is available.
     * If connectivity is already available, executes immediately.
     * Otherwise, schedules execution for when connectivity is restored.
     *
     * @param block The code to execute when connectivity is available
     * @return A job that can be cancelled if needed
     */
    fun runWhenConnected(block: suspend () -> Unit): Job {
        return serviceScope.launch {
            if (isConnected()) {
                launchIO { block() }
            } else {
                mutex.withLock {
                    pendingConnectivityBlocks.add(block)
                    log.d { "Added block to be run when connectivity restarts" }
                }
            }
        }
    }

    private fun isConnected(): Boolean {
        return webSocketClientProvider.get().isConnected()
    }
}