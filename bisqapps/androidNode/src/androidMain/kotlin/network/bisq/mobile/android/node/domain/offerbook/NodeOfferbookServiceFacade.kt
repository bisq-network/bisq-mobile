package network.bisq.mobile.android.node.domain.offerbook

import bisq.common.currency.Market
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.offerbook.market.MarketChannelSelectionService
import network.bisq.mobile.android.node.domain.offerbook.market.MarketListItemService
import network.bisq.mobile.android.node.domain.offerbook.offers.OfferbookListItemService
import network.bisq.mobile.client.replicated_model.common.currency.MarketListItem
import network.bisq.mobile.domain.offerbook.OfferbookListItem
import network.bisq.mobile.domain.offerbook.OfferbookMarket
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade

class NodeOfferbookServiceFacade(private val applicationServiceSupplier: AndroidApplicationService.Supplier) :
    OfferbookServiceFacade {

    // Dependencies


    // Properties
    override val marketListItemList: List<MarketListItem> get() = marketListItemService.marketListItems
    override val offerbookListItemList: StateFlow<List<OfferbookListItem>> get() = offerbookListItemService.offerbookListItems
    override val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = marketChannelSelectionService.selectedOfferbookMarket

    // Misc
    private val log = Logger.withTag(this::class.simpleName ?: "NodeOfferbookServiceFacade")
    private var offerbookListItemService: OfferbookListItemService =
        OfferbookListItemService(applicationServiceSupplier)
    private var marketListItemService: MarketListItemService =
        MarketListItemService(applicationServiceSupplier)
    private var marketChannelSelectionService: MarketChannelSelectionService =
        MarketChannelSelectionService(applicationServiceSupplier)

    // Life cycle
    override fun initialize() {
        marketListItemService.initialize()
        marketChannelSelectionService.initialize()
        offerbookListItemService.initialize()
    }

    override fun resume() {
        marketListItemService.resume()
        marketChannelSelectionService.resume()
        offerbookListItemService.resume()
    }

    override fun dispose() {
        marketListItemService.dispose()
        marketChannelSelectionService.dispose()
        offerbookListItemService.dispose()
    }

    // API
    override fun selectMarket(marketListItem: MarketListItem) {
        val market = Market(
            marketListItem.baseCurrencyCode,
            marketListItem.quoteCurrencyCode,
            marketListItem.baseCurrencyName, marketListItem.quoteCurrencyName
        )
        marketChannelSelectionService.selectMarket(market)
    }
}