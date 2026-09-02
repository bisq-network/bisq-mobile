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
import network.bisq.mobile.domain.utils.resultCatching

class ClientTradeRestrictingAlertServiceFacade(
    private val apiGateway: TradeRestrictingAlertApiGateway,
    private val json: Json,
) : TradeRestrictingAlertServiceFacade() {
    private val _alert = MutableStateFlow<AuthorizedAlertData?>(null)
    override val alert: StateFlow<AuthorizedAlertData?> = _alert.asStateFlow()

    override suspend fun activate() {
        super.activate()

        serviceScope.launch {
            resultCatching {
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
        // Not collectPayloads: an event without a payload means "no alert" here, whereas the shared
        // collector ignores it. A payload that does not decode is skipped and the current alert stays.
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                _alert.value = null
                return@collect
            }

            val decoded =
                WebSocketEventPayload.from<AuthorizedAlertDataDto?>(json, webSocketEvent) ?: return@collect
            _alert.value = decoded.payload?.toDomainOrNull()
        }
    }
}
