package network.bisq.mobile.domain.service.network_stats

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.service.ServiceFacade

abstract class ProfileStatsServiceFacade : ServiceFacade() {
    protected val _publishedProfilesCount = MutableStateFlow(0)
    val publishedProfilesCount: StateFlow<Int> = _publishedProfilesCount
}