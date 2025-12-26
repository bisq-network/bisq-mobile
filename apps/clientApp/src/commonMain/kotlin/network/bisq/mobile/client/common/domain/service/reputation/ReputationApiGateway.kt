package network.bisq.mobile.client.common.domain.service.reputation

import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.utils.Logging

class ReputationApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    private val basePath = "reputation"

    suspend fun getProfileAge(userProfileId: String): Result<Long?> = webSocketApiClient.get("$basePath/profile-age/$userProfileId")

    suspend fun subscribeUserReputation(): WebSocketEventObserver {
        try {
            return webSocketClientService.subscribe(Topic.REPUTATION)
        } catch (e: Exception) {
            log.e(e) { "Failed to subscribe to reputation events: ${e.message}" }
            throw e
        }
    }

    suspend fun getReputationScore(userProfileId: String): Result<ReputationScoreVO> = webSocketApiClient.get("$basePath/score/$userProfileId")
}
