package network.bisq.mobile.android.node.service.network

import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.network.ConnectivityService

class NodeConnectivityService(private val applicationService: AndroidApplicationService.Provider): ConnectivityService() {
    override fun isConnected(): Boolean {
        // TODO implement this and also the ConnectivityService#newRequestRoundTripTime() call needs to be applied when a P2P roundtrip call is done
        return true
//        return try {
//            log.d { "Connected nodes = ${applicationService.networkService.get().numberOfConnectedNodes}"}
//            applicationService.networkService.get().numberOfConnectedNodes > 0
//        } catch (e: Exception) {
//            log.w(e) { "Failed to get number of connected peers through data network service" }
//            false
//        }
    }
}