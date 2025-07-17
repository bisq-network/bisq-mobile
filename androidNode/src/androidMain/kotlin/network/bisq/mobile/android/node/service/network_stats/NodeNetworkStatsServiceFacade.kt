package network.bisq.mobile.android.node.service.network_stats

import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.network_stats.NetworkStatsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeNetworkStatsServiceFacade(
    private val applicationService: AndroidApplicationService.Provider
) : NetworkStatsServiceFacade(), Logging {

    override fun activate() {
        super.activate()
        
        // TODO: Access P2P network services to get real stats
        // val userService = applicationService.userService
        // val networkService = applicationService.networkService
        
        // Mock data for now
        _publishedProfilesCount.value = 2150

        log.d { "NodeNetworkStatsServiceFacade activated with mock data" }
    }

    override fun deactivate() {
        super.deactivate()
        log.d { "NodeNetworkStatsServiceFacade deactivated" }
    }
}