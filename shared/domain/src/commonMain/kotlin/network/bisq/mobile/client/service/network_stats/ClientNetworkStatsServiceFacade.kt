package network.bisq.mobile.client.service.network_stats

import network.bisq.mobile.domain.service.network_stats.NetworkStatsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientNetworkStatsServiceFacade : NetworkStatsServiceFacade(), Logging {

    override fun activate() {
        super.activate()
        
        // TODO: Implement WebSocket subscription for network stats
        // Mock data for now
        _publishedProfilesCount.value = 1275

        log.d { "ClientNetworkStatsServiceFacade activated with mock data" }
    }

    override fun deactivate() {
        super.deactivate()
        log.d { "ClientNetworkStatsServiceFacade deactivated" }
    }
}