package network.bisq.mobile.android.node.domain.market_price

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.common.observable.Pin
import bisq.presentation.formatters.PriceFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.offerbook.NodeOfferbookServiceFacade.Companion.toLibraryMarket
import network.bisq.mobile.android.node.domain.offerbook.NodeOfferbookServiceFacade.Companion.toReplicatedMarket
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.utils.Logging

class NodeMarketPriceServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    MarketPriceServiceFacade, Logging {

    // Dependencies
    private val marketPriceService: MarketPriceService by lazy {
        applicationService.bondedRolesService.get().marketPriceService
    }

    // Properties
    private val _selectedMarketPriceItem = MutableStateFlow(MarketPriceItem.EMPTY)
    override val selectedMarketPriceItem: StateFlow<MarketPriceItem> get() = _selectedMarketPriceItem

    private val _selectedFormattedMarketPrice = MutableStateFlow("N/A")
    override val selectedFormattedMarketPrice: StateFlow<String> = _selectedFormattedMarketPrice

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
        marketPricePin =
            marketPriceService.marketPriceByCurrencyMap.addObserver { updateMarketPriceItem() }
    }

    private fun observeSelectedMarket() {
        selectedMarketPin?.unbind()
        selectedMarketPin = marketPriceService.selectedMarket.addObserver { market ->
           try {
               updateMarketPriceItem()
           } catch (e: Exception) {
               log.e("Failed to update market item", e)
           }
        }
    }

    private fun updateMarketPriceItem() {
        val market = marketPriceService.selectedMarket.get()
        if (market != null) {
            marketPriceService.findMarketPriceQuote(market)
                .ifPresent { priceQuote ->
                    val replicatedMarket = toReplicatedMarket(market)
                    val formattedPrice = PriceFormatter.formatWithCode(priceQuote)
                    _selectedFormattedMarketPrice.value = formattedPrice
                    _selectedMarketPriceItem.value =
                        MarketPriceItem(replicatedMarket, priceQuote.value, formattedPrice)
                }
        }
    }
}