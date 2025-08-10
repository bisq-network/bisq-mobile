package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.service.ServiceFacade

/**
 * Base definition for the connectivity service. Each app type should implement / override the default
 * based on its network type.
 */
abstract class ConnectivityService : ServiceFacade() {
    companion object {
        const val TIMEOUT = 5000L
        const val PERIOD = 5000L // default check every 5 sec
        const val ROUND_TRIP_SLOW_THRESHOLD = 500L

        private const val DEFAULT_AVERAGE_TRIP_TIME = -1L // invalid
        const val MIN_REQUESTS_TO_ASSESS_SPEED = 3 // invalid

        private var sessionTotalRequests = 0L
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

    enum class ConnectivityStatus {
        DISCONNECTED,
        SLOW,
        CONNECTED
    }

    private val pendingConnectivityBlocks = mutableListOf<suspend () -> Unit>()
    private val mutex = Mutex()

    private var job: Job? = null
    private val _status = MutableStateFlow(ConnectivityStatus.DISCONNECTED)
    val status: StateFlow<ConnectivityStatus> get() = _status.asStateFlow()

    /**
     * Starts monitoring connectivity every given period (ms). Default is 10 seconds.
     * @param period of time in ms to check connectivity
     * @param startDelay to delay the first check, default to 5 secs
     */
    fun startMonitoring(period: Long = PERIOD, startDelay: Long = 5_000) {
        onStart()
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

    private suspend fun checkConnectivity() {
        try {
            withTimeout(TIMEOUT) {
                val previousStatus = _status.value
                _status.value = when {
                    !isConnected() -> ConnectivityStatus.DISCONNECTED
                    isSlow() -> ConnectivityStatus.SLOW
                    else -> ConnectivityStatus.CONNECTED
                }
                if (previousStatus != _status.value) {
                    log.d { "Connectivity transition from $previousStatus to ${_status.value}" }
                    if (previousStatus == ConnectivityStatus.DISCONNECTED) {
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
                        // fire&forget: Create a fresh scope for each block
                        CoroutineScope(IODispatcher + SupervisorJob()).launch {
                            block()
                        }
                    }
                }
            }
        }
    }

    fun stopMonitoring() {
        job?.cancel()
        job = null
        onStop()
        log.d { "Connectivity stopped being monitored" }
    }

    protected open fun onStart() {
        // default nth
    }

    protected open fun onStop() {
        // default nth
    }

    abstract fun isConnected(): Boolean

    /**
     * Default implementation uses round trip average measuring.
     * It relays on other components updating it on each request.
     */
    @Throws(IllegalStateException::class)
    protected open suspend fun isSlow(): Boolean {
        if (sessionTotalRequests > MIN_REQUESTS_TO_ASSESS_SPEED) {
//            log.d { "Current average trip time is ${averageTripTime}ms" }
            return averageTripTime > ROUND_TRIP_SLOW_THRESHOLD
        }
        return false // assume is not slow on non mature connections
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
}
