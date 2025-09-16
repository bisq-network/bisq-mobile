package network.bisq.mobile.android.node.service.network

import bisq.common.observable.Pin
import bisq.network.identity.NetworkId
import bisq.network.p2p.ServiceNode
import bisq.network.p2p.message.EnvelopePayloadMessage
import bisq.network.p2p.node.CloseReason
import bisq.network.p2p.node.Connection
import bisq.network.p2p.node.Node
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade

class NodeNetworkServiceFacade(private val provider: AndroidApplicationService.Provider) : NetworkServiceFacade, Node.Listener {
    private var defaultNode: Node? = null
    private var serviceNodeStatePin: Pin? = null

    // While tor starts up we use -1 to flag as network not available yet
    private val _numConnections = MutableStateFlow(-1)
    override val numConnections: StateFlow<Int> get() = _numConnections.asStateFlow()

    override fun activate() {
        super.activate()

        val networkService = provider.applicationService.networkService
        val serviceNodesByTransport = networkService.serviceNodesByTransport.serviceNodesByTransport
        // We only support one transport type in mobile
        serviceNodesByTransport.values.forEach { serviceNode ->
            serviceNodeStatePin = serviceNode.state.addObserver { state ->
                if (ServiceNode.State.INITIALIZING == state) {
                    defaultNode = serviceNode.defaultNode
                    requireNotNull(defaultNode) { "defaultNode is not null when state is ServiceNode.State.INITIALIZING" }
                    defaultNode!!.addListener(this)
                    updateNumConnections()

                    serviceNodeStatePin?.unbind()
                    serviceNodeStatePin = null
                }
            }
        }
    }

    override fun deactivate() {
        serviceNodeStatePin?.unbind()
        serviceNodeStatePin = null
        defaultNode?.removeListener(this)
        defaultNode = null
    }

    /* Node.Listener implementation */
    override fun onMessage(message: EnvelopePayloadMessage, connection: Connection, networkId: NetworkId) {
    }

    override fun onConnection(connection: Connection) {
        updateNumConnections()
    }

    override fun onDisconnect(connection: Connection, closeReason: CloseReason) {
        updateNumConnections()
    }

    private fun updateNumConnections() {
        // -1 if defaultNode not available
        _numConnections.value = defaultNode?.numConnections ?: -1
    }
}