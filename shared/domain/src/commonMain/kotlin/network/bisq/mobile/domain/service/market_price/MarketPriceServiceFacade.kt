package network.bisq.mobile.domain.service.market_price

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO

interface MarketPriceServiceFacade : LifeCycleAware {
    val selectedMarketPriceItem: StateFlow<MarketPriceItem?>
    val selectedFormattedMarketPrice: StateFlow<String>

    fun selectMarket(marketListItem: MarketListItem)
    fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem?
    fun findUSDMarketPriceItem(): MarketPriceItem?
    fun refreshSelectedFormattedMarketPrice()
}