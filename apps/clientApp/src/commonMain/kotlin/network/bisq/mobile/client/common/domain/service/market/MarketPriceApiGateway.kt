package network.bisq.mobile.client.common.domain.service.market

import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class MarketPriceApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    private val basePath = "market-price"

    // @Deprecated use subscription instead
    suspend fun getQuotes(): Result<QuotesResponse> = webSocketApiClient.get("$basePath/quotes")

    suspend fun subscribeMarketPrice(): WebSocketEventObserver = webSocketClientService.subscribe(Topic.MARKET_PRICE)
}
