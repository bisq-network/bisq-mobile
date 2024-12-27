package network.bisq.mobile.client.service.offerbook

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.service.offerbook.market.ClientMarketListItemService
import network.bisq.mobile.client.service.offerbook.market.ClientSelectedOfferbookMarketService
import network.bisq.mobile.client.service.offerbook.offer.ClientOfferbookListItemService
import network.bisq.mobile.client.service.offerbook.offer.OfferbookApiGateway
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferbookMarket
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientOfferbookServiceFacade(
    apiGateway: OfferbookApiGateway,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    json: Json
) :
    OfferbookServiceFacade, Logging {

    // Properties
    override val offerbookMarketItems: List<MarketListItem> get() = marketListItemService.marketListItems
    override val offerListItems: StateFlow<List<OfferListItemVO>> get() = offerbookListItemService.offerListItems
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
    override fun selectOfferbookMarket(marketListItem: MarketListItem) {
        marketPriceServiceFacade.selectMarket(marketListItem)
        selectedOfferbookMarketService.selectMarket(marketListItem)
        offerbookListItemService.selectMarket(marketListItem)
    }
}