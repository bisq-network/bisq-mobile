package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.CoroutineScope
import network.bisq.mobile.domain.data.BackgroundDispatcher

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import network.bisq.mobile.domain.utils.Logging

/**
 * Base definition for the connectivity service. Each app type should implement / override the default
 * based on its network type.
 */
abstract class ConnectivityService: Logging {
    companion object {
        const val TIMEOUT = 5000L
        const val PERIOD = 10000L // default check every 10 sec
        const val ROUND_TRIP_SLOW_THRESHOLD = 500L

        const val DEFAULT_AVERAGE_TRIP_TIME = -1L // invalid
        const val MIN_REQUESTS_TO_ASSESS_SPEED = 3 // invalid

        private var sessionTotalRequests = 0L
        private var averageTripTime = DEFAULT_AVERAGE_TRIP_TIME

        suspend fun newRequestRoundTripTime(timeInMs: Long) {
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

    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null
    private val _status = MutableStateFlow(ConnectivityStatus.DISCONNECTED)
    val status: StateFlow<ConnectivityStatus> = _status

    /**
     * Starts monitoring connectivity every given period (ms). Default is 10 seconds.
     */
    fun startMonitoring(period: Long = PERIOD) {
        onStart()
        job?.cancel()
        job = coroutineScope.launch(BackgroundDispatcher) {
            while (true) {
                try {
                    withTimeout(TIMEOUT) {
                        val currentStatus = _status.value
                        when {
                            !isConnected() -> _status.value = ConnectivityStatus.DISCONNECTED
                            isSlow() -> _status.value = ConnectivityStatus.SLOW
                            else -> _status.value = ConnectivityStatus.CONNECTED
                        }
                        if (currentStatus != _status.value) {
                            log.d { "Connectivity transition from $currentStatus to ${_status.value}" }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    log.e(e) { "Connectivity check timed out, assuming no connection" }
                    _status.value = ConnectivityStatus.DISCONNECTED
                } catch (e: Exception) {
                    log.e(e) { "Failed checking connectivity, assuming no connection" }
                    _status.value = ConnectivityStatus.DISCONNECTED
                }
                delay(period)
            }
        }
    }

    fun stopMonitoring() {
        job?.cancel()
        job = null
        onStop()
    }

    protected open fun onStart() {
        // default nth
    }

    protected open fun onStop() {
        // default nth
    }

    protected abstract fun isConnected(): Boolean

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
        return false // asume is not slow on non mature connections
    }
}