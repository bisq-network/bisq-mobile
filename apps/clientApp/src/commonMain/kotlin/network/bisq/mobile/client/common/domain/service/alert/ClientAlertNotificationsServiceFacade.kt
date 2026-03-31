package network.bisq.mobile.client.common.domain.service.alert

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.data.mapping.alert.toDomainOrNull
import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData

class ClientAlertNotificationsServiceFacade(
    private val apiGateway: AlertNotificationsApiGateway,
    private val json: Json,
) : AlertNotificationsServiceFacade() {
    private val _alerts = MutableStateFlow<List<AuthorizedAlertData>>(emptyList())
    override val alerts: StateFlow<List<AuthorizedAlertData>> = _alerts.asStateFlow()

    override suspend fun activate() {
        super.activate()

        serviceScope.launch {
            runCatching {
                subscribeAlerts()
            }.onFailure {
                log.w { "Failed to subscribe to authorized alerts" }
            }
        }
    }

    override suspend fun deactivate() {
        _alerts.value = emptyList()
        super.deactivate()
    }

    override fun dismissAlert(alertId: String) {
        serviceScope.launch {
            apiGateway
                .dismissAlert(alertId)
                .onSuccess {
                    _alerts.update { currentAlerts ->
                        currentAlerts.filterNot { alert -> alert.id == alertId }
                    }
                }.onFailure { error ->
                    log.e(error) { "Failed to dismiss authorized alert: $alertId" }
                }
        }
    }

    private suspend fun subscribeAlerts() {
        apiGateway
            .subscribeAlerts()
            .onSuccess { observer ->
                observer.webSocketEvent.collect { webSocketEvent ->
                    if (webSocketEvent?.deferredPayload == null) {
                        return@collect
                    }

                    runCatching {
                        WebSocketEventPayload
                            .from<List<AuthorizedAlertDataDto>>(json, webSocketEvent)
                            .payload
                            .mapNotNull(AuthorizedAlertDataDto::toDomainOrNull)
                    }.onSuccess { payload ->
                        _alerts.value = payload
                    }.onFailure { error ->
                        log.e(error) { "Failed to deserialize authorized alert payload; event ignored." }
                    }
                }
            }.onFailure {
                log.e(it) { "Failed to subscribe to authorized alert events" }
            }
    }
}
