package network.bisq.mobile.client.common.domain.service.alert

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

    suspend fun subscribeAlerts(): Result<WebSocketEventObserver> =
        runCatching {
            webSocketClientService.subscribe(Topic.ALERT_NOTIFICATIONS, "MOBILE_CLIENT")
        }

    suspend fun dismissAlert(alertId: String): Result<Unit> = webSocketApiClient.delete("$basePath/$alertId")
}
