package network.bisq.mobile.android.node.domain.offerbook

import bisq.common.currency.Market
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.offerbook.market.NodeMarketListItemService
import network.bisq.mobile.android.node.domain.offerbook.market.NodeSelectedOfferbookMarketService
import network.bisq.mobile.android.node.domain.offerbook.offer.NodeOfferbookListItemService
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferbookMarket
import network.bisq.mobile.utils.Logging

class NodeOfferbookServiceFacade(
    applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade
) :
    OfferbookServiceFacade, Logging {

    companion object {
        fun toLibraryMarket(marketListItem: MarketListItem) =
            Market(
                marketListItem.market.baseCurrencyCode,
                marketListItem.market.quoteCurrencyCode,
                marketListItem.market.baseCurrencyName,
                marketListItem.market.quoteCurrencyName
            )

        fun toReplicatedMarket(market: Market) =
            network.bisq.mobile.client.replicated_model.common.currency.Market(
                market.baseCurrencyCode,
                market.quoteCurrencyCode,
                market.baseCurrencyName,
                market.quoteCurrencyName
            )
    }
    // Properties
    override val offerbookMarketItems: List<MarketListItem> get() = marketListItemService.marketListItems
    override val offerListItems: StateFlow<List<OfferListItem>> get() = offerbookListItemService.offerListItems
    override val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = selectedOfferbookMarketService.selectedOfferbookMarket

    // Misc
    private val offerbookListItemService: NodeOfferbookListItemService =
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
    override fun selectMarket(marketListItem: MarketListItem) {
        selectedOfferbookMarketService.selectMarket(toLibraryMarket(marketListItem))
        //todo marketPriceServiceFacade should not be managed here but on a higher level or from the presenter
        marketPriceServiceFacade.selectMarket(marketListItem)
    }
}