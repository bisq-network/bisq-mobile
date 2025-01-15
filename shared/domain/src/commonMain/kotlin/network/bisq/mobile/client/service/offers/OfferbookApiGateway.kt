package network.bisq.mobile.client.service.offers

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.utils.Logging

class OfferbookApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientProvider: WebSocketClientProvider,
) : Logging {
    private val basePath = "offerbook"

    // Requests
    suspend fun getMarkets(): Result<List<MarketVO>> {
        return webSocketApiClient.get("$basePath/markets")
    }

    suspend fun getNumOffersByMarketCode(): Result<Map<String, Int>> {
        return webSocketApiClient.get("$basePath/markets/offers/count")
    }

    suspend fun getOffers(code: String): Result<List<OfferItemPresentationDto>> {
        return webSocketApiClient.get("$basePath/markets/$code/offers")
    }

    suspend fun deleteOffer(offerId: String): Result<Unit> {
        return webSocketApiClient.delete("$basePath/offers/$offerId")
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
        return webSocketApiClient.post("$basePath/offers", createOfferRequest)
    }


    // Subscriptions
    suspend fun subscribeNumOffers(): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.NUM_OFFERS)
    }

    /**
     * @param code  The quote currency code for which we want to receive updates.
     *              If null or empty string we receive for all markets the offer updates.
     */
    suspend fun subscribeOffers(code: String? = null): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.OFFERS, code)
    }
}

