package network.bisq.mobile.client.common.domain.service.alert

import network.bisq.mobile.client.common.domain.APP_TYPE
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class TradeRestrictingAlertApiGateway(
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    suspend fun subscribeAlert(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.TRADE_RESTRICTING_ALERT, APP_TYPE)
}
