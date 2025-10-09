package network.bisq.mobile.client.service.market

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class MarketPriceApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientProvider: WebSocketClientProvider,
) : Logging {
    private val basePath = "market-price"

    // @Deprecated use subscription instead
    suspend fun getQuotes(): Result<QuotesResponse> {
        return webSocketApiClient.get("$basePath/quotes")
    }

    suspend fun subscribeMarketPrice(): WebSocketEventObserver {
        return webSocketClientProvider.subscribe(Topic.MARKET_PRICE)
    }
}
