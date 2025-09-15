package network.bisq.mobile.android.node.service.network

import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.network.ConnectivityService

class NodeConnectivityService(
    private val nodeNetworkServiceFacade: NodeNetworkServiceFacade
) : ConnectivityService() {

    // We get activated after application service is initialized. At that stage we expect to have at least connections.
    override fun activate() {
        serviceScope.launch {
            nodeNetworkServiceFacade.numConnections.collect { numConnections ->
                _status.value = if (numConnections == 0) {
                    ConnectivityStatus.DISCONNECTED
                } else if (numConnections <= 2) {
                    ConnectivityStatus.WARN
                } else {
                    ConnectivityStatus.CONNECTED
                }
            }
        }
    }

    override fun deactivate() {}
}
