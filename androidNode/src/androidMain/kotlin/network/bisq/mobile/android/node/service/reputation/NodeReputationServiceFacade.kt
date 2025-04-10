package network.bisq.mobile.android.node.service.reputation

import bisq.user.reputation.ReputationService
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeReputationServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    ReputationServiceFacade, Logging {
    private val reputationService: ReputationService by lazy {
        applicationService.reputationService.get()
    }

    // API
    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        val reputation = reputationService.getReputationScore(userProfileId).let {
            Mappings.ReputationScoreMapping.fromBisq2Model(it)
        }
        return Result.success(reputation)
    }

    override suspend fun getScoreByUserProfileId(): Result<Map<String, Long>> {
        return Result.success(reputationService.scoreByUserProfileId)
    }

}