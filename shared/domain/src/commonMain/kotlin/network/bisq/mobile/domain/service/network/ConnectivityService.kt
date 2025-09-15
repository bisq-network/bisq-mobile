package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.BaseService

/**
 * Base definition for the connectivity service. Each app type should implement / override the default
 * based on its network type.
 */
abstract class ConnectivityService : BaseService() {
    enum class ConnectivityStatus {
        DISCONNECTED,
        WARN,
        CONNECTED
    }

    protected open val _status = MutableStateFlow(ConnectivityStatus.DISCONNECTED)
    val status: StateFlow<ConnectivityStatus> get() = _status.asStateFlow()

    abstract fun isConnected(): Boolean
}
