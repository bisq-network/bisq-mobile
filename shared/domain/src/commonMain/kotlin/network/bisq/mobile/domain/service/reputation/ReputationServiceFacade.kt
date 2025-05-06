package network.bisq.mobile.domain.service.reputation

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO

interface ReputationServiceFacade : LifeCycleAware {

    val reputationByUserProfileId: StateFlow<Map<String, ReputationScoreVO>>

    suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO>

}