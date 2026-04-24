package network.bisq.mobile.data.service.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.BaseService
import network.bisq.mobile.data.service.LifeCycleAware
import network.bisq.mobile.i18n.i18n

/**
 * Base definition for the connectivity service. Each app type should implement / override the default
 * based on its network type.
 *
 * This class **owns the RECONNECTING → DISCONNECTED transition**: a [serviceScope] coroutine starts
 * when the status becomes [ConnectivityStatus.RECONNECTING] and, after
 * [maxReconnectingDurationMs] without a status change, sets
 * [ConnectivityStatus.DISCONNECTED].
 */
abstract class ConnectivityService :
    BaseService(),
    LifeCycleAware {
    /**
     * How long to stay in [ConnectivityStatus.RECONNECTING] before the base class forces
     * [ConnectivityStatus.DISCONNECTED]. Subclasses can override (e.g. for tests or tuning).
     */
    open val maxReconnectingDurationMs: Long
        get() = MAX_RECONNECTING_DURATION_MS

    private var reconnectingTimeoutJob: Job? = null

    /**
     * After a successful timeout, raw RECONNECTING from a subclass is mapped to DISCONNECTED until
     * connectivity is restored (any non-RECONNECTING [setConnectivityStatus]).
     */
    private var isReconnectingTimedOut: Boolean = false

    enum class ConnectivityStatus(
        val i18nKey: String,
    ) {
        BOOTSTRAPPING("mobile.connectivity.enum.bootstrapping"),
        DISCONNECTED("mobile.connectivity.enum.disconnected"), // startup / never had all inventory, or after prolonged reconnecting (incl. timeout) while the stack may still retry
        RECONNECTING("mobile.connectivity.enum.reconnecting"), // after inventory data have been received and then all connections lost. Usually after longer background.
        REQUESTING_INVENTORY("mobile.connectivity.enum.requesting_inventory"), // while requesting inventory data
        CONNECTED_AND_DATA_RECEIVED("mobile.connectivity.enum.connected_and_data_received"), // have received all inventory data and has connections
        CONNECTED_WITH_LIMITATIONS("mobile.connectivity.enum.connected_with_limitations"), // some of the requirements were not met by this connection
        ;

        fun isConnected() =
            when (this) {
                REQUESTING_INVENTORY,
                CONNECTED_AND_DATA_RECEIVED,
                CONNECTED_WITH_LIMITATIONS,
                -> true

                else -> false
            }

        fun i18n() = this.i18nKey.i18n()
    }

    private val _status = MutableStateFlow(ConnectivityStatus.BOOTSTRAPPING)
    val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

    protected fun setConnectivityStatus(candidateStatus: ConnectivityStatus) {
        when (candidateStatus) {
            ConnectivityStatus.RECONNECTING -> {
                if (isReconnectingTimedOut) {
                    if (_status.value != ConnectivityStatus.DISCONNECTED) {
                        _status.value = ConnectivityStatus.DISCONNECTED
                    }
                    return
                }
                if (_status.value == ConnectivityStatus.RECONNECTING) {
                    return
                }
                cancelReconnectingTimeoutJob()
                _status.value = ConnectivityStatus.RECONNECTING
                var scheduledJob: Job? = null
                scheduledJob =
                    serviceScope.launch {
                        try {
                            delay(maxReconnectingDurationMs)
                        } catch (e: CancellationException) {
                            return@launch
                        }
                        if (_status.value == ConnectivityStatus.RECONNECTING) {
                            isReconnectingTimedOut = true
                            _status.value = ConnectivityStatus.DISCONNECTED
                        }
                        if (reconnectingTimeoutJob === scheduledJob) {
                            reconnectingTimeoutJob = null
                        }
                    }
                reconnectingTimeoutJob = scheduledJob
            }
            else -> {
                cancelReconnectingTimeoutJob()
                isReconnectingTimedOut = false
                _status.value = candidateStatus
            }
        }
    }

    private fun cancelReconnectingTimeoutJob() {
        reconnectingTimeoutJob?.cancel()
        reconnectingTimeoutJob = null
    }

    private fun onLifecycleShutdown() {
        cancelReconnectingTimeoutJob()
        isReconnectingTimedOut = false
    }

    override suspend fun activate() {
        onLifecycleShutdown()
        _status.value = ConnectivityStatus.BOOTSTRAPPING
    }

    override suspend fun deactivate() {
        onLifecycleShutdown()
        _status.value = ConnectivityStatus.BOOTSTRAPPING
    }

    companion object {
        const val MAX_RECONNECTING_DURATION_MS: Long = 3 * 60 * 1000L
    }
}
