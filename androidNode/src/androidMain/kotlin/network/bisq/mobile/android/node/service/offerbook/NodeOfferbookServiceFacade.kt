package network.bisq.mobile.android.node.service.offerbook

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.android.node.service.offerbook.market.NodeMarketListItemService
import network.bisq.mobile.android.node.service.offerbook.market.NodeSelectedOfferbookMarketService
import network.bisq.mobile.android.node.service.offerbook.offer.NodeOfferbookListItemService
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferbookMarket
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.domain.utils.Logging


class NodeOfferbookServiceFacade(
    applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade
) :
    OfferbookServiceFacade, Logging {

    // Properties
    override val offerbookMarketItems: List<MarketListItem> get() = marketListItemService.marketListItems
    override val offerListItems: StateFlow<List<OfferListItemVO>> get() = offerbookListItemService.offerListItems
    override val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = selectedOfferbookMarketService.selectedOfferbookMarket

    // Misc
    val offerbookListItemService: NodeOfferbookListItemService =
        NodeOfferbookListItemService(applicationService)
    private val marketListItemService: NodeMarketListItemService =
        NodeMarketListItemService(applicationService)
    private val selectedOfferbookMarketService: NodeSelectedOfferbookMarketService =
        NodeSelectedOfferbookMarketService(applicationService, marketPriceServiceFacade)

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
        selectedOfferbookMarketService.selectMarket(Mappings.MarketMapping.toPojo(marketListItem.market))
        //todo marketPriceServiceFacade should not be managed here but on a higher level or from the presenter
        marketPriceServiceFacade.selectMarket(marketListItem)
    }
}