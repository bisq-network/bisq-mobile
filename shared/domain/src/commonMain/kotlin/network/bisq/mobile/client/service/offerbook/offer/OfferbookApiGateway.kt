package network.bisq.mobile.client.service.offerbook.offer

import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.utils.Logging

class OfferbookApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClient: WebSocketClient,
) : Logging {
    private val basePath = "offerbook"

    // Requests
    suspend fun getMarkets(): Result<List<MarketVO>> {
        return webSocketApiClient.get("$basePath/markets")
    }

    suspend fun getNumOffersByMarketCode(): Result<Map<String, Int>> {
        return webSocketApiClient.get("$basePath/markets/offers/count")
    }

    suspend fun getOffers(code: String): Result<List<OfferListItemVO>> {
        return webSocketApiClient.get("$basePath/markets/$code/offers")
    }

    // Subscriptions
    suspend fun subscribeNumOffers(): WebSocketEventObserver? {
        return webSocketClient.subscribe(Topic.NUM_OFFERS)
    }

    /**
     * @param code  The quote currency code for which we want to receive updates.
     *              If null or empty string we receive for all markets the offer updates.
     */
    suspend fun subscribeOffers(code: String? = null): WebSocketEventObserver? {
        return webSocketClient.subscribe(Topic.OFFERS, code)
    }
}

