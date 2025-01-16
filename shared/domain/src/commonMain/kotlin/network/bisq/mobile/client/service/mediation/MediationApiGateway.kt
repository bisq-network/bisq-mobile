package network.bisq.mobile.client.service.mediation

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging

class MediationApiGateway(
    private val webSocketApiClient: WebSocketApiClient
) : Logging {
    private val basePath = "mediation"

    suspend fun reportToMediator(tradeId: String): Result<Unit> {
        //todo backend not impl yet
        return webSocketApiClient.put("$basePath/selected", tradeId)
    }
}

