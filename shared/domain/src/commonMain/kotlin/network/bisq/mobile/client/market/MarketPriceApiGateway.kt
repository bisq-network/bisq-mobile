package network.bisq.mobile.client.market

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.utils.Logging

class MarketPriceApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClient: WebSocketClient,
) : Logging {
    private val basePath = "market-price"

    suspend fun getQuotes(): MarketPriceResponse {
        return webSocketApiClient.get("$basePath/quotes")
    }

    suspend fun subscribeMarketPrice(): WebSocketEventObserver? {
        return webSocketClient.subscribe(Topic.MARKET_PRICE)
    }

    @Serializable
    data class MarketPriceResponse(val quotes: Map<String, Long>)
}

