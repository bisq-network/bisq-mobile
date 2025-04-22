package network.bisq.mobile.android.node.service.reputation

import bisq.user.reputation.ReputationService
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade

class NodeReputationServiceFacade(private val applicationService: AndroidApplicationService.Provider) : ServiceFacade(),
    ReputationServiceFacade {
    private val reputationService: ReputationService by lazy { applicationService.reputationService.get() }

    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

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