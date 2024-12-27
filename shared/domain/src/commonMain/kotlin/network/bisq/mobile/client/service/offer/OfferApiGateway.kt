package network.bisq.mobile.client.service.offer

import network.bisq.mobile.client.service.trade.TakeOfferRequest
import network.bisq.mobile.client.service.trade.TakeOfferResponse
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.utils.Logging

class OfferApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClient: WebSocketClient,
) : Logging {
    private val basePath = "offers"

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
        return webSocketApiClient.post("trades", takeOfferRequest)
    }

    suspend fun publishOffer(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>
    ): Result<CreateOfferResponse> {
        val createOfferRequest = CreateOfferRequest(
            direction,
            market,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            amountSpec,
            priceSpec,
            supportedLanguageCodes
        )
        return webSocketApiClient.post(basePath, createOfferRequest)
    }
}

