package network.bisq.mobile.android.node.service.network

import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.network.ConnectivityService

@Suppress("RedundantOverride")
class NodeConnectivityService(private val applicationService: AndroidApplicationService.Provider) : ConnectivityService() {

    companion object {
        // With production average of ~10 peers, threshold of 4 provides good balance:
        // - Allows for some peer churn without triggering "slow" status
        // - Maintains sufficient redundancy for P2P network reliability
        // - Represents ~40% of typical peer count as minimum acceptable
        const val SLOW_PEER_QUANTITY_THRESHOLD = 4
    }

    override fun isConnected(): Boolean {
        val connections = currentConnections()
        log.v { "Connected peers = $connections" }
        return connections > 0
    }

    override suspend fun isSlow(): Boolean {
        // Note: Round-trip time measurement is complex for P2P networks due to:
        // - Multiple peers with different characteristics
        // - Async message patterns without clear request-response correlation
        // - Transport abstraction (CLEAR/TOR) hiding timing details
        // Connection count is a simpler and more reliable indicator for P2P network health
        return currentConnections() < SLOW_PEER_QUANTITY_THRESHOLD
    }

    private fun currentConnections(): Int {
        try {
            return applicationService.networkService.get()
                .serviceNodesByTransport.allServiceNodes
                .sumOf { it.defaultNode?.numConnections ?: 0 }
        } catch (e: Exception) {
            log.e(e) { "Failed to check current connections, assuming none: ${e.message}" }
            return 0
        }
    }
}