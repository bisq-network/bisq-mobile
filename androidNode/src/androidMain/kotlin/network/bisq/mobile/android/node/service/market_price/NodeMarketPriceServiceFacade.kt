package network.bisq.mobile.android.node.service.market_price

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.common.observable.Pin
import bisq.presentation.formatters.PriceFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeMarketPriceServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    MarketPriceServiceFacade, Logging {

    // Dependencies
    private val marketPriceService: MarketPriceService by lazy {
        applicationService.bondedRolesService.get().marketPriceService
    }

    // Properties
    private val _selectedMarketPriceItem: MutableStateFlow<MarketPriceItem?> = MutableStateFlow(null)
    override val selectedMarketPriceItem: StateFlow<MarketPriceItem?> get() = _selectedMarketPriceItem

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
        val market = Mappings.MarketMapping.toPojo(marketListItem.market)
        marketPriceService.setSelectedMarket(market)
    }

    override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? {
        val market = Mappings.MarketMapping.toPojo(marketVO)
        return marketPriceService.findMarketPrice(market)
            .map { it.priceQuote }
            .map { Mappings.PriceQuoteMapping.from(it) }
            .map { priceQuoteVO ->
                val formattedPrice = MarketPriceFormatter.format(priceQuoteVO.value, marketVO)
                MarketPriceItem(marketVO, priceQuoteVO, formattedPrice)
            }
            .orElse(null)
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
                    val marketVO = Mappings.MarketMapping.from(market)
                    val formattedPrice = PriceFormatter.formatWithCode(priceQuote)
                    _selectedFormattedMarketPrice.value = formattedPrice
                    val priceQuoteVO = Mappings.PriceQuoteMapping.from(priceQuote)
                    _selectedMarketPriceItem.value = MarketPriceItem(marketVO, priceQuoteVO, formattedPrice)
                }
        }
    }
}