package network.bisq.mobile.android.node.service.network_stats

import bisq.common.observable.Pin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.service.network_stats.ProfileStatsServiceFacade
import network.bisq.mobile.domain.utils.Logging
import kotlin.time.Duration.Companion.seconds

class NodeProfileStatsServiceFacade(
    private val applicationService: AndroidApplicationService.Provider
) : ProfileStatsServiceFacade(), Logging {

    companion object {
        private val UPDATE_THROTTLE_DELAY = 2.seconds
    }

    private var job: Job? = null
    private var publishedProfilesCountPin: Pin? = null
    private var throttleJob: Job? = null
    private var pendingCount: Int? = null

    override fun activate() {
        super.activate()

        observePublishedProfilesCount()

        log.d { "NodeNetworkStatsServiceFacade activated" }
    }

    override fun deactivate() {
        publishedProfilesCountPin?.unbind()
        publishedProfilesCountPin = null
        throttleJob?.cancel()
        throttleJob = null
        pendingCount = null
        super.deactivate()
        log.d { "NodeNetworkStatsServiceFacade deactivated" }
    }

    private fun observePublishedProfilesCount() {
        publishedProfilesCountPin?.unbind()
        publishedProfilesCountPin = null
        job?.cancel()
        throttleJob?.cancel()
        job = launchIO {
            val userService = applicationService.userService
            val userProfileService = userService.get().userProfileService
            publishedProfilesCountPin = userProfileService.numUserProfiles.addObserver { num ->
                // Throttle rapid updates to prevent memory pressure
                pendingCount = num ?: 0

                launchPublishedProfilesThrottledUpdateJob()
            }
        }
    }

    private fun launchPublishedProfilesThrottledUpdateJob() {
        throttleJob?.cancel()
        throttleJob = launchIO {
            delay(UPDATE_THROTTLE_DELAY)
            pendingCount?.let { count ->
                _publishedProfilesCount.value = count
                log.d { "Published profiles count updated (throttled): $count" }
            }
        }
    }
}