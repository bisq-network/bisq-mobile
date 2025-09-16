package network.bisq.mobile.client.service.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.service.network.NetworkServiceFacade

class ClientNetworkServiceFacade(private val webSocketClientProvider: WebSocketClientProvider) : NetworkServiceFacade {

    // While tor starts up we use -1 to flag as network not available yet
    private val _numConnections = MutableStateFlow(-1)
    override val numConnections: StateFlow<Int> get() = _numConnections.asStateFlow()

    override fun activate() {
        // TODO implement gateway and endpoints to subscribe to number of connections of backend
    }

    override fun deactivate() {
    }

    private fun updateNumConnections() {
        // -1 if defaultNode not available
        //_numConnections.value = defaultNode?.numConnections ?: -1
    }
}