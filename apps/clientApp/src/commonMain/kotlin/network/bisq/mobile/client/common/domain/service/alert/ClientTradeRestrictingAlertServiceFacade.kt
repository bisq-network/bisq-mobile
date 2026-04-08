package network.bisq.mobile.client.common.domain.service.alert

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.data.mapping.alert.toDomainOrNull
import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData

class ClientTradeRestrictingAlertServiceFacade(
    private val apiGateway: TradeRestrictingAlertApiGateway,
    private val json: Json,
) : TradeRestrictingAlertServiceFacade() {
    private val _alert = MutableStateFlow<AuthorizedAlertData?>(null)
    override val alert: StateFlow<AuthorizedAlertData?> = _alert.asStateFlow()

    override suspend fun activate() {
        super.activate()

        serviceScope.launch {
            runCatching {
                subscribeAlert()
            }.onFailure {
                log.w { "Failed to subscribe to trade restricting alert" }
            }
        }
    }

    override suspend fun deactivate() {
        super.deactivate()
        _alert.value = null
    }

    private suspend fun subscribeAlert() {
        val observer = apiGateway.subscribeAlert()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                _alert.value = null
                return@collect
            }

            runCatching {
                WebSocketEventPayload
                    .from<AuthorizedAlertDataDto?>(json, webSocketEvent)
                    .payload
                    ?.toDomainOrNull()
            }.onSuccess { payload ->
                _alert.value = payload
            }.onFailure { error ->
                log.e(error) { "Failed to deserialize trade restricting alert payload; event ignored." }
            }
        }
    }
}
