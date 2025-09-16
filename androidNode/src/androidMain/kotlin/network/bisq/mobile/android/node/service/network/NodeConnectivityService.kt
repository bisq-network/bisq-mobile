package network.bisq.mobile.android.node.service.network

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.network.ConnectivityService

class NodeConnectivityService(
    private val nodeNetworkServiceFacade: NodeNetworkServiceFacade
) : ConnectivityService() {

    // Activated after application service is initialized.
    private var collectJob: Job? = null
    override fun activate() {
        collectJob = serviceScope.launch {
            nodeNetworkServiceFacade.numConnections.collect { numConnections ->
                _status.value = when {
                    numConnections < 0 -> ConnectivityStatus.BOOTSTRAPPING // Not expected case
                    numConnections == 0 -> ConnectivityStatus.DISCONNECTED
                    numConnections <= 2 -> ConnectivityStatus.WARN
                    else -> ConnectivityStatus.CONNECTED
                }
            }
        }
    }

    override fun deactivate() {
        collectJob?.cancel()
        collectJob = null
    }
}
