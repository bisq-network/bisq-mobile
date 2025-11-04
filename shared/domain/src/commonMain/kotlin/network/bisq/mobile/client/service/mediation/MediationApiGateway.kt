package network.bisq.mobile.client.service.mediation

import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging

class MediationApiGateway(
    private val webSocketApiClient: WebSocketApiClient
) : Logging {

    suspend fun reportToMediator(tradeId: String): Result<Unit> {
        return webSocketApiClient.post("trades/$tradeId/mediation", "")
    }
}

