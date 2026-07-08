package network.bisq.mobile.node.common.domain.service.network

import bisq.common.network.TransportType
import bisq.common.observable.Pin
import bisq.network.identity.NetworkId
import bisq.network.p2p.ServiceNode
import bisq.network.p2p.message.EnvelopePayloadMessage
import bisq.network.p2p.node.CloseReason
import bisq.network.p2p.node.Connection
import bisq.network.p2p.node.Node
import bisq.network.p2p.services.peer_group.PeerGroupService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import kotlin.streams.toList

class NodeNetworkServiceFacade(
    private val provider: AndroidApplicationService.Provider,
    kmpTorService: KmpTorService,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
) : NetworkServiceFacade(kmpTorService, applicationBootstrapFacade),
    Node.Listener {
    // While tor starts up we use -1 to flag as network not available yet
    private val _numConnections = MutableStateFlow(-1)
    override val numConnections: StateFlow<Int> = _numConnections.asStateFlow()

    private val _allDataReceived = MutableStateFlow(false)
    override val allDataReceived: StateFlow<Boolean> = _allDataReceived.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<NodePeerInfo>>(emptyList())
    val connectedPeers: StateFlow<List<NodePeerInfo>> = _connectedPeers.asStateFlow()

    private val _myNodeAddress = MutableStateFlow<String?>(null)
    val myNodeAddress: StateFlow<String?> = _myNodeAddress.asStateFlow()

    private var defaultNode: Node? = null
    private var peerGroupService: PeerGroupService? = null
    private var serviceNodeStatePin: Pin? = null
    private var allDataReceivedPin: Pin? = null

    override suspend fun isTorEnabled(): Boolean {
        val networkServiceConfig = provider.applicationService.networkServiceConfig
        return networkServiceConfig?.supportedTransportTypes?.contains(TransportType.TOR) ?: false
    }

    override suspend fun activate() {
        super.activate()
        val networkService = provider.applicationService.networkService
        val serviceNodesByTransport = networkService.serviceNodesByTransport.serviceNodesByTransport
        // We only support one transport type in mobile
        require(serviceNodesByTransport.size == 1) {
            "Expected exactly one transport type on mobile, found ${serviceNodesByTransport.size}"
        }
        serviceNodesByTransport.values.forEach { serviceNode ->
            serviceNodeStatePin =
                serviceNode.state.addObserver { state ->
                    log.i { "ServiceNode state changed to: $state, defaultNode: ${serviceNode.defaultNode}" }
                    if (ServiceNode.State.INITIALIZING == state) {
                        defaultNode = serviceNode.defaultNode
                        requireNotNull(defaultNode) { "defaultNode must not be null when state is ServiceNode.State.INITIALIZING" }
                        log.i { "Setting up Node.Listener for defaultNode: $defaultNode" }
                        defaultNode!!.addListener(this)
                        peerGroupService = serviceNode.peerGroupManager.map { it.peerGroupService }.orElse(null)
                        updateNumConnections()
                        updateConnectedPeers()
                        updateMyNodeAddress()

                        observeInventoryData(serviceNode)

                        serviceNodeStatePin?.unbind()
                        serviceNodeStatePin = null
                    }
                }
        }
    }

    override suspend fun deactivate() {
        super.deactivate()
        serviceNodeStatePin?.unbind()
        serviceNodeStatePin = null
        defaultNode?.removeListener(this)
        defaultNode = null
        peerGroupService = null
        _connectedPeers.value = emptyList()
        _myNodeAddress.value = null

        allDataReceivedPin?.unbind()
        allDataReceivedPin = null
    }

    // Node.Listener implementation
    override fun onMessage(
        message: EnvelopePayloadMessage,
        connection: Connection,
        networkId: NetworkId,
    ) {
    }

    override fun onConnection(connection: Connection) {
        log.i { "onConnection: ${connection.peerAddress}, total: ${defaultNode?.numConnections ?: -1}" }
        updateNumConnections()
        updateConnectedPeers()
        updateMyNodeAddress()
    }

    override fun onDisconnect(
        connection: Connection,
        closeReason: CloseReason,
    ) {
        log.i { "onDisconnect: ${connection.peerAddress}, reason: $closeReason, total: ${defaultNode?.numConnections ?: -1}" }
        updateNumConnections()
        updateConnectedPeers()
    }

    private fun updateNumConnections() {
        // -1 if defaultNode not available
        _numConnections.value = defaultNode?.numConnections ?: -1
    }

    private fun updateConnectedPeers() {
        val node = defaultNode
        _connectedPeers.value =
            if (node == null) {
                emptyList()
            } else {
                node.allActiveConnections.toList().map { it.toNodePeerInfo() }
            }
        log.d { "connectedPeers updated: ${_connectedPeers.value.size} peers" }
    }

    private fun updateMyNodeAddress() {
        // The node's own onion address is stable once the server binds, so resolve it once.
        if (_myNodeAddress.value != null) return
        _myNodeAddress.value = defaultNode?.findMyAddress()?.map { it.fullAddress }?.orElse(null)
        log.d { "myNodeAddress resolved: ${_myNodeAddress.value != null}" }
    }

    private fun Connection.toNodePeerInfo(): NodePeerInfo =
        NodePeerInfo(
            connectionId = id,
            address = peerAddress.fullAddress,
            isOutbound = isOutboundConnection,
            establishedAtMillis = created,
            isSeed = peerGroupService?.isSeed(this) ?: false,
        )

    private fun observeInventoryData(serviceNode: ServiceNode) {
        if (serviceNode.inventoryService.isEmpty) {
            return
        }
        val inventoryService = serviceNode.inventoryService.get()

        allDataReceivedPin =
            inventoryService.numPendingInventoryRequests.addObserver { numPendingRequests ->
                log.d { "Node inventory pending requests: $numPendingRequests" }
                _allDataReceived.value = numPendingRequests == 0
            }
    }
}
