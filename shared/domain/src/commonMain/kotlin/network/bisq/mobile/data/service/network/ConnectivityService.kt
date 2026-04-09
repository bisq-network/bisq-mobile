package network.bisq.mobile.data.service.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.BaseService
import network.bisq.mobile.data.service.LifeCycleAware
import network.bisq.mobile.i18n.i18n

/**
 * Base definition for the connectivity service. Each app type should implement / override the default
 * based on its network type.
 */
abstract class ConnectivityService :
    BaseService(),
    LifeCycleAware {
    enum class ConnectivityStatus(
        val i18nKey: String,
    ) {
        BOOTSTRAPPING("mobile.connectivity.enum.bootstrapping"),
        DISCONNECTED("mobile.connectivity.enum.disconnected"), // never had received all inventory data, usually at startup if no connections are established
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

    protected open val _status = MutableStateFlow(ConnectivityStatus.BOOTSTRAPPING)
    val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

    override suspend fun activate() {
        _status.value = ConnectivityStatus.BOOTSTRAPPING
    }

    override suspend fun deactivate() {
        _status.value = ConnectivityStatus.BOOTSTRAPPING
    }
}
