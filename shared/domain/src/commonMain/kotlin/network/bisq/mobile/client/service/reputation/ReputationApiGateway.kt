package network.bisq.mobile.client.service.reputation

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.utils.Logging

class ReputationApiGateway(
    private val webSocketClientProvider: WebSocketClientProvider,
    private val webSocketApiClient: WebSocketApiClient
) : Logging {
    private val basePath = "reputation"
    suspend fun getReputationScore(userProfileId: String): Result<ReputationScoreVO> {
        return webSocketApiClient.get("$basePath/score/$userProfileId")
    }

    suspend fun getProfileAge(userProfileId: String): Result<Long?> {
        return webSocketApiClient.get("$basePath/profile-age/$userProfileId")
    }

    suspend fun subscribeUserReputation(): WebSocketEventObserver {
        try {
            return webSocketClientProvider.get().subscribe(Topic.USER_REPUTATION)
        } catch (e: Exception) {
            log.e(e) { "Failed to subscribe to reputation events: ${e.message}" }
            throw e
        }
    }
}