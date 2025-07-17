package network.bisq.mobile.android.node.service.network_stats

import bisq.common.observable.Pin
import kotlinx.coroutines.Job
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.network_stats.NetworkStatsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeNetworkStatsServiceFacade(
    private val applicationService: AndroidApplicationService.Provider
) : NetworkStatsServiceFacade(), Logging {

    private var job: Job? = null
    private var publishedProfilesCountPin: Pin? = null

    override fun activate() {
        super.activate()

        observePublishedProfilesCount()

        log.d { "NodeNetworkStatsServiceFacade activated" }
    }

    override fun deactivate() {
        publishedProfilesCountPin?.unbind()
        publishedProfilesCountPin = null
        super.deactivate()
        log.d { "NodeNetworkStatsServiceFacade deactivated" }
    }

    private fun observePublishedProfilesCount() {
        job?.cancel()
        job = launchIO {
            val userService = applicationService.userService
            val userProfileService = userService.get().userProfileService

            publishedProfilesCountPin?.unbind()
            publishedProfilesCountPin = userProfileService.numUserProfiles.addObserver { num ->
                _publishedProfilesCount.value = num
                log.d { "Published profiles count updated: ${_publishedProfilesCount.value}" }
            }
        }
    }
}