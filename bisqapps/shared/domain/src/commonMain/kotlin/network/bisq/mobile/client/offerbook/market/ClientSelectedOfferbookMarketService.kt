package network.bisq.mobile.client.offerbook.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.offerbook.market.MarketListItem
import network.bisq.mobile.domain.offerbook.market.OfferbookMarket
import network.bisq.mobile.utils.Logging

class ClientSelectedOfferbookMarketService(
    private val marketPriceServiceFacade: MarketPriceServiceFacade
) :
    LifeCycleAware, Logging {

    // Properties
    private val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket.EMPTY)
    val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket

    // Misc
    private var selectedMarketListItem: MarketListItem = MarketListItem.USD
    private var marketPriceObserverJob: Job? = null

    // Life cycle
    override fun activate() {
        marketPriceObserverJob = observeMarketPrice()
    }

    override fun deactivate() {
        marketPriceObserverJob?.cancel()
    }

    // API
    fun selectMarket(marketListItem: MarketListItem) {
        this.selectedMarketListItem = marketListItem
        log.i { "selectMarket " + marketListItem }
        _selectedOfferbookMarket.value = OfferbookMarket(marketListItem.market)

        marketPriceObserverJob?.cancel()
        marketPriceObserverJob = observeMarketPrice()
    }

    private fun observeMarketPrice(): Job {
        return CoroutineScope(BackgroundDispatcher).launch {
            marketPriceServiceFacade.marketPriceItem.collectLatest { marketPriceItem ->
                _selectedOfferbookMarket.value.setFormattedPrice(marketPriceItem.formattedPrice.value)
            }
        }
    }
}