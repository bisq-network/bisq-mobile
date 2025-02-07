package network.bisq.mobile.android.node.service.network

import network.bisq.mobile.domain.service.network.ConnectivityService

class NodeConnectivityService(): ConnectivityService() {
    override fun isConnected(): Boolean {
        // TODO implement
        return true
    }

    override suspend fun isSlow(): Boolean {
        // TODO implement
        return false
    }

}