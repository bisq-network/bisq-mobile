package network.bisq.mobile.client.service.reputation

import network.bisq.mobile.client.service.mediation.ReputationApiGateway
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientReputationServiceFacade(val apiGateway: ReputationApiGateway) : ReputationServiceFacade, Logging {
    // API
    override suspend fun getReputation(userId: String): Result<ReputationScoreVO> {
        return apiGateway.getReputation(userId)
    }

    override suspend fun getScoreByUserProfileId(): Result<Map<String, Long>> {
        return apiGateway.getScoreByUserProfileId()
    }
}