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
    // While tor starts up we use -1 to flag as network not available yet
    private val _numConnections = MutableStateFlow(-1)
    override val numConnections: StateFlow<Int> get() = _numConnections.asStateFlow()

    private val _targetNumConnectedPeers = MutableStateFlow(0)
    override val targetNumConnectedPeers: StateFlow<Int> get() = _targetNumConnectedPeers.asStateFlow()

    private val _numPendingRequests = MutableStateFlow(0)
    override val numPendingRequests: StateFlow<Int> get() = _numPendingRequests.asStateFlow()

    private val _allDataReceived = MutableStateFlow(false)
    override val allDataReceived: StateFlow<Boolean> get() = _allDataReceived.asStateFlow()

    private val _maxPendingRequestsAtStartup = MutableStateFlow(0)
    override val maxPendingRequestsAtStartup: StateFlow<Int> get() = _maxPendingRequestsAtStartup.asStateFlow()

    private var defaultNode: Node? = null
    private var serviceNodeStatePin: Pin? = null
    private var numPendingRequestsPin: Pin? = null
    private var allDataReceivedPin: Pin? = null

    override fun activate() {
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

                    require(serviceNode.peerGroupManager.isPresent) { "peerGroupManager must be present" }
                    _targetNumConnectedPeers.value = serviceNode.peerGroupManager.get().peerGroupService.targetNumConnectedPeers

                    observeInventoryData(serviceNode)

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

        numPendingRequestsPin?.unbind()
        numPendingRequestsPin = null
        allDataReceivedPin?.unbind()
        allDataReceivedPin = null
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

    private fun observeInventoryData(serviceNode: ServiceNode) {
        if (serviceNode.inventoryService.isEmpty) {
            return
        }

        val inventoryService = serviceNode.inventoryService.get()
        val inventoryRequestService = inventoryService.inventoryRequestService

        _maxPendingRequestsAtStartup.value = inventoryService.config.maxPendingRequestsAtStartup

        numPendingRequestsPin = inventoryRequestService.numPendingRequests.addObserver { numPendingRequests ->
            _numPendingRequests.value = numPendingRequests
        }

        allDataReceivedPin = inventoryRequestService.allDataReceived.addObserver { allDataReceived ->
            if (allDataReceived) {
                _allDataReceived.value = allDataReceived

                allDataReceivedPin?.unbind()
                allDataReceivedPin = null
            }
        }
    }
}