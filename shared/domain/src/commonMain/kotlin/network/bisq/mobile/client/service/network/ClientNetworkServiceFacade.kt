package network.bisq.mobile.client.service.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.client.websocket.WebSocketClientService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade

class ClientNetworkServiceFacade(private val webSocketClientService: WebSocketClientService) : NetworkServiceFacade() {

    // While tor starts up we use -1 to flag as network not available yet
    private val _numConnections = MutableStateFlow(-1)
    override val numConnections: StateFlow<Int> get() = _numConnections.asStateFlow()

    private val _allDataReceived = MutableStateFlow(false)
    override val allDataReceived: StateFlow<Boolean> get() = _allDataReceived.asStateFlow()

    override suspend fun activate() {
        super.activate()
        // TODO implement gateway and endpoints to subscribe to number of connections of backend
    }

    override suspend fun deactivate() {
        super.deactivate()
    }
}
