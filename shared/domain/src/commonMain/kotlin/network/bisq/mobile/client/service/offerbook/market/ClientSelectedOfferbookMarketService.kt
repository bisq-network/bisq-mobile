package network.bisq.mobile.client.service.offerbook.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferbookMarket
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientSelectedOfferbookMarketService(
    private val marketPriceServiceFacade: MarketPriceServiceFacade
) :
    LifeCycleAware, Logging {

    // Properties
    private val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket.EMPTY)
    val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket

    // Misc
    private var selectedMarketListItem: MarketListItem = MarketListItem.USD
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null

    // Life cycle
    override fun activate() {
        job = observeMarketPrice()
    }

    override fun deactivate() {
        cancelJob()
    }

    // API
    fun selectMarket(marketListItem: MarketListItem) {
        this.selectedMarketListItem = marketListItem
        log.i { "selectMarket " + marketListItem }
        _selectedOfferbookMarket.value = OfferbookMarket(marketListItem.market)

        cancelJob()
        job = observeMarketPrice()
    }


    private fun observeMarketPrice(): Job {
        return coroutineScope.launch {
            marketPriceServiceFacade.selectedMarketPriceItem.collectLatest { marketPriceItem ->
                if (marketPriceItem != null) {
                    _selectedOfferbookMarket.value.setFormattedPrice(marketPriceItem.formattedPrice)
                }
            }
        }
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}