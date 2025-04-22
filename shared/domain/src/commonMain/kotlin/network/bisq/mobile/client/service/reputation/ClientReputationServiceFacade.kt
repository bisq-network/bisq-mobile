package network.bisq.mobile.client.service.reputation

import network.bisq.mobile.client.service.mediation.ReputationApiGateway
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade

class ClientReputationServiceFacade(val apiGateway: ReputationApiGateway) : ServiceFacade(), ReputationServiceFacade {
    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        return apiGateway.getReputation(userProfileId)
    }

    override suspend fun getScoreByUserProfileId(): Result<Map<String, Long>> {
        return apiGateway.getScoreByUserProfileId()
    }
}