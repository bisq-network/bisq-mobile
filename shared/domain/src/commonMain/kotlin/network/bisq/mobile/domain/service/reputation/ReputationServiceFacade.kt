package network.bisq.mobile.domain.service.reputation

import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO

interface ReputationServiceFacade {
    suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO>

    suspend fun getScoreByUserProfileId(): Result<Map<String, Long>>
}