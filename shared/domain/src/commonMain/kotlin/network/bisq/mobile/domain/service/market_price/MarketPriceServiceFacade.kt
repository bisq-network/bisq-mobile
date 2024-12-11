package network.bisq.mobile.domain.service.market_price

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.MarketPriceItem

interface MarketPriceServiceFacade : LifeCycleAware {
    val selectedMarketPriceItem: StateFlow<MarketPriceItem>
    val selectedFormattedMarketPrice: StateFlow<String>

    fun selectMarket(marketListItem: MarketListItem)
}