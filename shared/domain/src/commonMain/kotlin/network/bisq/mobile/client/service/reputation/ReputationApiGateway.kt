package network.bisq.mobile.client.service.reputation

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class ReputationApiGateway(
    private val webSocketClientProvider: WebSocketClientProvider,
) : Logging {
    suspend fun subscribeUserReputation(): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.REPUTATION)
    }
}