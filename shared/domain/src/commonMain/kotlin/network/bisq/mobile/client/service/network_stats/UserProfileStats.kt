package network.bisq.mobile.client.service.network_stats

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class UserProfileStats(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientProvider: WebSocketClientProvider,
) : Logging {
    suspend fun subscribeNetworkStats(): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.USER_PROFILE_STATS)
    }
}