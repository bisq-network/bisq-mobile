package network.bisq.mobile.client.service.mediation

import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.utils.Logging

class ReputationApiGateway(
    private val webSocketApiClient: WebSocketApiClient
) : Logging {
    private val basePath = "reputation"

    suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> {
        return webSocketApiClient.get("$basePath/scores/$userProfileId")
    }

    suspend fun getScoreByUserProfileId(): Result<Map<String, Long>> {
        return webSocketApiClient.get("$basePath/scores")
    }
}