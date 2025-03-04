package network.bisq.mobile.android.node.service.network

import bisq.common.network.TransportType
import bisq.network.identity.NetworkId
import bisq.network.p2p.message.EnvelopePayloadMessage
import bisq.network.p2p.node.CloseReason
import bisq.network.p2p.node.Connection
import bisq.network.p2p.node.Node
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.network.ConnectivityService

class NodeConnectivityService(private val applicationService: AndroidApplicationService.Provider): ConnectivityService() {
    // TODO ConnectivityService#newRequestRoundTripTime() call needs to be applied when a P2P roundtrip call is done for the parent isSlow impl to work

    private var connections = 0
    private val nodeListener : Node.Listener = object: Node.Listener {
            override fun onMessage(epm: EnvelopePayloadMessage?, conn: Connection?, network: NetworkId?) {
                // do nth
            }

            override fun onConnection(connection: Connection?) {
                log.d { "connection stablished $connection" }
                connections++
            }

            override fun onDisconnect(connection: Connection?, closeReason: CloseReason?) {
                if (connections > 0) {
                    log.d { "connection lost $connection for $closeReason" }
                    connections--
                }
            }

        }

    override fun onStart() {
        try {
            val serviceNode = applicationService.networkService.get().findServiceNode(TransportType.CLEAR).get()
            serviceNode.nodesById.addNodeListener(nodeListener)
        } catch (e: Exception) {
            log.w(e) { "failed to start node monitoring for connectivity service" }
        }
    }

    override fun onStop() {
        try {
            val serviceNode = applicationService.networkService.get().findServiceNode(TransportType.CLEAR).get()
            serviceNode.nodesById.removeNodeListener(nodeListener)
        } catch (e: Exception) {
            log.w(e) { "failed to start node monitoring for connectivity service" }
        }
    }

    override fun isConnected(): Boolean {
        log.d { "Connected peers = $connections"}
        return connections > 0
    }
}