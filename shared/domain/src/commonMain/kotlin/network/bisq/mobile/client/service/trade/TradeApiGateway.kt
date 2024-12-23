package network.bisq.mobile.client.service.trade

import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging

class TradeApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClient: WebSocketClient,
) : Logging {
    private val basePath = "trades"

    suspend fun takeOffer(
        offerId: String,
        baseSideAmount: Long,
        quoteSideAmount: Long,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String
    ): Result<TakeOfferResponse> {
        val takeOfferRequest = TakeOfferRequest(
            offerId,
            baseSideAmount,
            quoteSideAmount,
            bitcoinPaymentMethod,
            fiatPaymentMethod
        )
        return webSocketApiClient.post(basePath, takeOfferRequest)
    }
}

