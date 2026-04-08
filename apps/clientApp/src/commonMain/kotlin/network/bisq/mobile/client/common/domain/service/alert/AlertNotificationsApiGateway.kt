package network.bisq.mobile.client.common.domain.service.alert

import network.bisq.mobile.client.common.domain.APP_TYPE
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class AlertNotificationsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    private val basePath = "alert-notifications"

    suspend fun subscribeAlerts(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.ALERT_NOTIFICATIONS, APP_TYPE)

    suspend fun dismissAlert(alertId: String): Result<Unit> = webSocketApiClient.delete("$basePath/$alertId?appType=$APP_TYPE")
}
