package network.bisq.mobile.android.node.domain.market_price

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.common.observable.Pin
import bisq.presentation.formatters.PriceFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.offerbook.NodeOfferbookServiceFacade.Companion.toLibraryMarket
import network.bisq.mobile.android.node.domain.offerbook.NodeOfferbookServiceFacade.Companion.toReplicatedMarket
import network.bisq.mobile.domain.data.model.market_price.MarketPriceItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.data.model.offerbook.market.MarketListItem
import network.bisq.mobile.utils.Logging

class NodeMarketPriceServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    MarketPriceServiceFacade, Logging {

    // Dependencies
    private val marketPriceService: MarketPriceService by lazy {
        applicationService.bondedRolesService.get().marketPriceService
    }

    // Properties
    private val _marketPriceItem = MutableStateFlow(MarketPriceItem.EMPTY)
    override val marketPriceItem: StateFlow<MarketPriceItem> get() = _marketPriceItem

    // Misc
    private var selectedMarketPin: Pin? = null
    private var marketPricePin: Pin? = null

    // Life cycle
    override fun activate() {
        observeSelectedMarket()
        observeMarketPrice()
    }

    override fun deactivate() {
        selectedMarketPin?.unbind()
        selectedMarketPin = null
        marketPricePin?.unbind()
        marketPricePin = null
    }

    // API
    override fun selectMarket(marketListItem: MarketListItem) {
        marketPriceService.setSelectedMarket(toLibraryMarket(marketListItem))
    }

    // Private
    private fun observeMarketPrice() {
        marketPricePin = marketPriceService.marketPriceByCurrencyMap.addObserver { updatePrice() }
    }

    private fun observeSelectedMarket() {
        selectedMarketPin?.unbind()
        selectedMarketPin = marketPriceService.selectedMarket.addObserver { market ->
            _marketPriceItem.value = MarketPriceItem(toReplicatedMarket(market))
            updatePrice()
        }
    }

    private fun updatePrice() {
        marketPriceService.findMarketPriceQuote(marketPriceService.selectedMarket.get())
            .ifPresent { priceQuote ->
                _marketPriceItem.value.setQuote(priceQuote.value)
                val formattedPrice = PriceFormatter.format(priceQuote)
                _marketPriceItem.value.setFormattedPrice(formattedPrice)
            }
    }
}