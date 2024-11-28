package network.bisq.mobile.domain.service.market_price

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.MarketListItem

interface MarketPriceServiceFacade : LifeCycleAware {
    val marketPriceItem: StateFlow<MarketPriceItem>
    fun selectMarket(marketListItem: MarketListItem)
}