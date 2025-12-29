package network.bisq.mobile.node.common.domain.service.network

import bisq.common.network.TransportType
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
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeNetworkServiceFacade(
    private val provider: AndroidApplicationService.Provider,
    kmpTorService: KmpTorService,
) : NetworkServiceFacade(kmpTorService),
    Node.Listener {
    // While tor starts up we use -1 to flag as network not available yet
    private val _numConnections = MutableStateFlow(-1)
    override val numConnections: StateFlow<Int> get() = _numConnections.asStateFlow()

    private val _allDataReceived = MutableStateFlow(false)
    override val allDataReceived: StateFlow<Boolean> get() = _allDataReceived.asStateFlow()

    private var defaultNode: Node? = null
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
                        updateNumConnections()

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
    }

    override fun onDisconnect(
        connection: Connection,
        closeReason: CloseReason,
    ) {
        log.i { "onDisconnect: ${connection.peerAddress}, reason: $closeReason, total: ${defaultNode?.numConnections ?: -1}" }
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

        allDataReceivedPin =
            inventoryService.numPendingInventoryRequests.addObserver { numPendingRequests ->
                log.d { "Node inventory pending requests: $numPendingRequests" }
                _allDataReceived.value = numPendingRequests == 0
            }
    }
}
