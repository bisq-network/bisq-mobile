package network.bisq.mobile.client.offerbook.offer

import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.utils.Logging

class OfferbookApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClient: WebSocketClient,
) : Logging {
    private val basePath = "offerbook"

    // Requests
    suspend fun getMarkets(): List<Market> {
        return webSocketApiClient.get("$basePath/markets")
    }

    suspend fun getNumOffersByMarketCode(): Map<String, Int> {
        return webSocketApiClient.get("$basePath/markets/offers/count")
    }

    suspend fun getOffers(code: String): List<OfferListItem> {
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

