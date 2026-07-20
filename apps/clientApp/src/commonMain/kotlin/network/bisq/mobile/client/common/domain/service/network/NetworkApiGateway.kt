package network.bisq.mobile.client.common.domain.service.network

import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class NetworkApiGateway(
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    suspend fun subscribeNetworkInfo(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.NETWORK_INFO)
}
