package network.bisq.mobile.client.common.domain.service.alert

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.data.mapping.alert.toDomainOrNull
import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.client.common.domain.websocket.subscription.collectPayloads
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.domain.utils.resultCatching

class ClientAlertNotificationsServiceFacade(
    private val apiGateway: AlertNotificationsApiGateway,
    private val json: Json,
) : AlertNotificationsServiceFacade() {
    private val _alerts = MutableStateFlow<List<AuthorizedAlertData>>(emptyList())
    override val alerts: StateFlow<List<AuthorizedAlertData>> = _alerts.asStateFlow()

    override suspend fun activate() {
        super.activate()

        serviceScope.launch {
            resultCatching {
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
        val observer = apiGateway.subscribeAlerts()
        observer.collectPayloads<List<AuthorizedAlertDataDto>>(json) { payload, _ ->
            _alerts.value = payload.mapNotNull(AuthorizedAlertDataDto::toDomainOrNull)
        }
    }
}
