package network.bisq.mobile.client.common.domain.service.mediation

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging

class MediationApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    suspend fun reportToMediator(tradeId: String): Result<Unit> = webSocketApiClient.post("trades/$tradeId/mediation", "")
}
