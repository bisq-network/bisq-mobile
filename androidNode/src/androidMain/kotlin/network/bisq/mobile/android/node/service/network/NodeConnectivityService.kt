package network.bisq.mobile.android.node.service.network

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.network.ConnectivityService

class NodeConnectivityService(
    private val nodeNetworkServiceFacade: NodeNetworkServiceFacade
) : ConnectivityService() {

    private var hasOnceReceivedAllData: Boolean = false
    private var collectJob: Job? = null

    // Activated after application service is initialized.
    override fun activate() {
        collectJob?.cancel()
        collectJob = serviceScope.launch {
            combine(nodeNetworkServiceFacade.numConnections, nodeNetworkServiceFacade.allDataReceived) { numConnections, allDataReceived ->
                numConnections to allDataReceived
            }.collect { (numConnections, allDataReceived) ->
                // allDataReceived in the network layer will get reset to false when we lose all connections.
                // We want to keep the information if we have ever received all data, as we distinguish then to show the reconnect
                // overlay instead of the connections lost dialogue which is used when bootstrap fails.
                if (allDataReceived && !hasOnceReceivedAllData) {
                    hasOnceReceivedAllData = true
                }

                if (numConnections <= 0) {
                    if (hasOnceReceivedAllData) {
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
