package network.bisq.mobile.client.offerbook

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.offerbook.market.ClientMarketListItemService
import network.bisq.mobile.client.offerbook.market.ClientSelectedOfferbookMarketService
import network.bisq.mobile.client.offerbook.offer.ClientOfferbookListItemService
import network.bisq.mobile.client.offerbook.offer.OfferbookApiGateway
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.data.model.OfferbookMarket
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.utils.Logging

class ClientOfferbookServiceFacade(
    apiGateway: OfferbookApiGateway,
    webSocketClient: WebSocketClient,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val json: Json
) :
    OfferbookServiceFacade, Logging {

    // Properties
    override val offerbookMarketItems: List<MarketListItem> get() = marketListItemService.marketListItems
    override val offerListItems: StateFlow<List<OfferListItem>> get() = offerbookListItemService.offerListItems
    override val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = selectedOfferbookMarketService.selectedOfferbookMarket

    // Misc
    private val offerbookListItemService: ClientOfferbookListItemService =
        ClientOfferbookListItemService(apiGateway, json)
    private val marketListItemService: ClientMarketListItemService =
        ClientMarketListItemService(apiGateway, json)
    private val selectedOfferbookMarketService: ClientSelectedOfferbookMarketService =
        ClientSelectedOfferbookMarketService(marketPriceServiceFacade)

    // Life cycle
    override fun activate() {
        marketListItemService.activate()
        selectedOfferbookMarketService.activate()
        offerbookListItemService.activate()
    }

    override fun deactivate() {
        marketListItemService.deactivate()
        selectedOfferbookMarketService.deactivate()
        offerbookListItemService.deactivate()
    }

    // API
    override fun selectMarket(marketListItem: MarketListItem) {
        marketPriceServiceFacade.selectMarket(marketListItem)
        selectedOfferbookMarketService.selectMarket(marketListItem)
        offerbookListItemService.selectMarket(marketListItem)
    }
}