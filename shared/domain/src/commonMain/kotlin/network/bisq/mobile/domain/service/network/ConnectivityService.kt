package network.bisq.mobile.domain.service.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.service.BaseService

/**
 * Base definition for the connectivity service. Each app type should implement / override the default
 * based on its network type.
 */
abstract class ConnectivityService : BaseService(), LifeCycleAware {
    enum class ConnectivityStatus {
        BOOTSTRAPPING,
        DISCONNECTED,
        WARN,
        CONNECTED
    }

    protected open val _status = MutableStateFlow(ConnectivityStatus.BOOTSTRAPPING)
    val status: StateFlow<ConnectivityStatus> get() = _status.asStateFlow()
}
