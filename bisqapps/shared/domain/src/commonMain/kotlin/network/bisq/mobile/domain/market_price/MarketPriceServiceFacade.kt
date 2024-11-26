package network.bisq.mobile.domain.market_price

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.offerbook.market.MarketListItem

interface MarketPriceServiceFacade : LifeCycleAware {
    val marketPriceItem: StateFlow<MarketPriceItem>
    fun selectMarket(marketListItem: MarketListItem)
}