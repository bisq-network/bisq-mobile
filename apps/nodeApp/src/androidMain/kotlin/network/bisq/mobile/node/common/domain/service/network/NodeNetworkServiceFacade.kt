package network.bisq.mobile.node.common.domain.service.network

import bisq.common.network.TransportType
import bisq.common.observable.Pin
import bisq.identity.Identity
import bisq.network.identity.NetworkId
import bisq.network.p2p.ServiceNode
import bisq.network.p2p.message.EnvelopePayloadMessage
import bisq.network.p2p.node.CloseReason
import bisq.network.p2p.node.Connection
import bisq.network.p2p.node.Node
import bisq.network.p2p.services.peer_group.PeerGroupService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.coroutines.DispatcherProvider
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import kotlin.math.roundToLong
import kotlin.streams.toList

class NodeNetworkServiceFacade(
    private val provider: AndroidApplicationService.Provider,
    kmpTorService: KmpTorService,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val dispatcherProvider: DispatcherProvider,
) : NetworkServiceFacade(kmpTorService, applicationBootstrapFacade),
    Node.Listener {
    private companion object {
        // While traffic keeps arriving we re-snapshot the peer list on this fixed cadence (sample, not
        // debounce, so a sustained burst can't starve the refresh — see startPeerStateRefresh).
        // Deliberately coarse: these are debugging-tier counters and each refresh maps every Connection.
        const val METRICS_REFRESH_INTERVAL_MS = 3_000L
    }

    // While tor starts up we use -1 to flag as network not available yet
    private val _numConnections = MutableStateFlow(-1)
    override val numConnections: StateFlow<Int> = _numConnections.asStateFlow()

    private val _allDataReceived = MutableStateFlow(false)
    override val allDataReceived: StateFlow<Boolean> = _allDataReceived.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<NodePeerInfo>>(emptyList())
    val connectedPeers: StateFlow<List<NodePeerInfo>> = _connectedPeers.asStateFlow()

    private val _myNodeInfo = MutableStateFlow(NodeInfo())
    val myNodeInfo: StateFlow<NodeInfo> = _myNodeInfo.asStateFlow()

    private var defaultNode: Node? = null
    private var peerGroupService: PeerGroupService? = null
    private var serviceNodeStatePin: Pin? = null
    private var allDataReceivedPin: Pin? = null

    // Per-peer metrics (RTT/bytes) are plain mutable counters on the Connection's ConnectionMetrics.
    // The desktop app refreshes them by attaching a Connection.Listener per peer and updating each row on
    // its own onNetworkMessage (fires on receive). We deliberately take a coarser, simpler approach: a single
    // node-wide Node.Listener feeds the two signals below, and ONE collector coroutine owns every write to
    // _numConnections / _connectedPeers / _myNodeInfo. That single ownership is what makes the read-then-write
    // in refreshPeerState() safe — see startPeerStateRefresh(). Trades refresh latency for a much simpler
    // lifecycle (one job, no per-connection add/remove bookkeeping).
    //
    // peerSetTick: connect/disconnect. Always live and unsampled — _numConnections feeds the network status
    //              indicator, so this arm must keep running with no Connections screen attached.
    // metricsTick: inbound traffic. Sampled AND gated on _connectedPeers having a collector, so the node does
    //              no metrics work while the Connections screen is closed (the common case).
    //
    // Both use DROP_OLDEST so tryEmit never fails: collapsing ticks is safe because the collector re-snapshots
    // the whole peer list rather than applying a delta.
    private val peerSetTick =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val metricsTick =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var peerStateRefreshJob: Job? = null

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
                // getState(), not .state: ServiceNode exposes both a public field and an accessor, and
                // Kotlin resolves the property syntax to the raw field, bypassing the accessor.
                serviceNode.getState().addObserver { state ->
                    log.i { "ServiceNode state changed to: $state, defaultNode: ${serviceNode.defaultNode}" }
                    if (ServiceNode.State.INITIALIZING == state) {
                        defaultNode = serviceNode.defaultNode
                        requireNotNull(defaultNode) { "defaultNode must not be null when state is ServiceNode.State.INITIALIZING" }
                        log.i { "Setting up Node.Listener for defaultNode: $defaultNode" }
                        defaultNode!!.addListener(this)
                        peerGroupService = serviceNode.peerGroupManager.map { it.peerGroupService }.orElse(null)
                        // Takes the initial snapshot itself, so there is exactly one writer from the start.
                        startPeerStateRefresh()

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
        // super.deactivate() cancels serviceScope, but refreshPeerState() has no suspension points: once
        // started it runs to completion and would write a stale snapshot AFTER the clearing below. Join the
        // (already cancelled) job so the collector is provably done before we reset the flows.
        peerStateRefreshJob?.cancelAndJoin()
        peerStateRefreshJob = null
        _connectedPeers.value = emptyList()
        _myNodeInfo.value = NodeInfo()

        allDataReceivedPin?.unbind()
        allDataReceivedPin = null
    }

    // Node.Listener implementation
    override fun onMessage(
        message: EnvelopePayloadMessage,
        connection: Connection,
        networkId: NetworkId,
    ) {
        // Signal that per-peer metrics likely changed; the collector in startPeerStateRefresh() re-snapshots
        // the peer list. tryEmit is non-blocking and never fails (DROP_OLDEST).
        metricsTick.tryEmit(Unit)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun startPeerStateRefresh() {
        if (peerStateRefreshJob?.isActive == true) return
        peerStateRefreshJob =
            serviceScope.launch(dispatcherProvider.default) {
                // Initial snapshot inside the coroutine rather than via a tick: a SharedFlow with replay=0
                // drops emissions made before this collector subscribes.
                refreshPeerState()
                merge(
                    peerSetTick,
                    _connectedPeers.subscriptionCount
                        .map { it > 0 }
                        .distinctUntilChanged()
                        .flatMapLatest { watched ->
                            if (watched) {
                                // flowOf(Unit) refreshes immediately on attach so the screen doesn't open on
                                // counters left over from the last connect/disconnect. sample (not debounce):
                                // a sustained inbound burst still refreshes on a fixed cadence instead of
                                // waiting for traffic to fall silent.
                                merge(flowOf(Unit), metricsTick.sample(METRICS_REFRESH_INTERVAL_MS))
                            } else {
                                emptyFlow()
                            }
                        },
                ).collect { refreshPeerState() }
            }
    }

    /**
     * The only writer of [_numConnections], [_connectedPeers] and [_myNodeInfo]; runs on one coroutine.
     *
     * Best-effort: the bisq2 getters read here race the node's network threads (#1796: the Node 0.10.0 jar,
     * before bisq2 fix 6707f25 / PR #4894, threw ConcurrentModificationException from ConnectionMetrics).
     * A throw escaping here would end this collector for the rest of the session and show a "Coroutine
     * operation failed" panel, so log and skip; the next tick re-snapshots everything. The three flows are
     * not atomic (_numConnections is written first); only _connectedPeers is never half-written.
     */
    private fun refreshPeerState() {
        try {
            updateNumConnections()
            updateConnectedPeers()
            updateMyNodeInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Peer state refresh failed; keeping the collector alive for the next tick" }
        }
    }

    override fun onConnection(connection: Connection) {
        log.i { "onConnection: ${connection.peerAddress}, total: ${defaultNode?.numConnections ?: -1}" }
        peerSetTick.tryEmit(Unit)
    }

    override fun onDisconnect(
        connection: Connection,
        closeReason: CloseReason,
    ) {
        log.i { "onDisconnect: ${connection.peerAddress}, reason: $closeReason, total: ${defaultNode?.numConnections ?: -1}" }
        peerSetTick.tryEmit(Unit)
    }

    private fun updateNumConnections() {
        // -1 if defaultNode not available
        _numConnections.value = defaultNode?.numConnections ?: -1
    }

    private fun updateConnectedPeers() {
        val node = defaultNode
        val peers =
            if (node == null) {
                emptyList()
            } else {
                node.allActiveConnections.toList().map { it.toNodePeerInfo() }
            }
        // Safe read-then-write: refreshPeerState() is the single writer (see startPeerStateRefresh).
        val previousCount = _connectedPeers.value.size
        _connectedPeers.value = peers
        // Only on peer-set changes: the metrics cadence would otherwise log every refresh interval.
        if (peers.size != previousCount) {
            log.d { "connectedPeers updated: ${peers.size} peers" }
        }
    }

    private fun updateMyNodeInfo() {
        val current = _myNodeInfo.value
        val node = defaultNode
        // keyId is available as soon as defaultNode is set; the onion address only after the server binds. Resolve each once.
        val keyId = current.keyId ?: node?.networkId?.keyId
        val address = current.onionAddress ?: node?.findMyAddress()?.map { it.fullAddress }?.orElse(null)
        // nodeTag is the local identity's tag (own identity, same on every connection); resolve once.
        val nodeTag =
            current.nodeTag
                ?: node?.let {
                    provider.applicationService.identityService
                        .findAnyIdentityByNetworkId(it.networkId)
                        .map(Identity::getTag)
                        .orElse(null)
                }
        val updated = NodeInfo(onionAddress = address, keyId = keyId, nodeTag = nodeTag)
        if (updated != current) {
            _myNodeInfo.value = updated
            log.d { "myNodeInfo resolved: address=${address != null}, keyId=${keyId != null}" }
        }
    }

    private fun Connection.toNodePeerInfo(): NodePeerInfo {
        val metrics = connectionMetrics
        return NodePeerInfo(
            connectionId = id,
            address = peerAddress.fullAddress,
            isOutbound = isOutboundConnection,
            establishedAtMillis = created,
            isSeed = peerGroupService?.isSeed(this) ?: false,
            // averageRtt is 0 until a round-trip is measured (handshake / request-response); treat that as "unmeasured".
            // Null-check the raw average, not the rounded value: a measured sub-millisecond rtt still rounds to 0.
            rttMillis = metrics.averageRtt.takeIf { it > 0 }?.roundToLong(),
            sentBytes = metrics.sentBytes,
            sentMessageCount = metrics.numMessagesSent,
            receivedBytes = metrics.receivedBytes,
            receivedMessageCount = metrics.numMessagesReceived,
        )
    }

    private fun observeInventoryData(serviceNode: ServiceNode) {
        if (serviceNode.inventoryService.isEmpty) {
            return
        }
        val inventoryService = serviceNode.inventoryService.get()

        allDataReceivedPin =
            inventoryService.initialInventoryRequestsCompleted.addObserver {
                log.d { "Node inventory initial requests completed: $it" }
                _allDataReceived.value = it
            }
    }
}
