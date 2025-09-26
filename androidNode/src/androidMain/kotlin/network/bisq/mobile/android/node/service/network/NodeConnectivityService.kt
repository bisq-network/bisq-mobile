package network.bisq.mobile.android.node.service.network

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.network.ConnectivityService

class NodeConnectivityService(
    private val nodeNetworkServiceFacade: NodeNetworkServiceFacade
) : ConnectivityService() {

    // Activated after application service is initialized.
    private var collectJob: Job? = null
    override fun activate() {
        collectJob?.cancel()
        collectJob = serviceScope.launch {
            combine(nodeNetworkServiceFacade.numConnections, nodeNetworkServiceFacade.allDataReceived) { numConnections, allDataReceived ->
                numConnections to allDataReceived
            }.collect { (numConnections, allDataReceived) ->
                if (numConnections <= 0) {
                    if (allDataReceived) {
                        _status.value = ConnectivityStatus.RECONNECTING
                    } else {
                        _status.value = ConnectivityStatus.DISCONNECTED
                    }
                } else {
                    if (allDataReceived) {
                        _status.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
                    } else {
                        _status.value = ConnectivityStatus.REQUESTING_INVENTORY
                    }
                }
            }
        }
    }

    override fun deactivate() {
        collectJob?.cancel()
        collectJob = null
    }
}
