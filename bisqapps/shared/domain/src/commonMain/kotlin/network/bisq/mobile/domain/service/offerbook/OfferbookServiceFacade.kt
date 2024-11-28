package network.bisq.mobile.domain.service.offerbook

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferbookMarket

interface OfferbookServiceFacade: LifeCycleAware {
    val offerbookMarketItems: List<MarketListItem>
    val offerListItems: StateFlow<List<OfferListItem>>
    val selectedOfferbookMarket: StateFlow<OfferbookMarket>

    fun selectMarket(marketListItem: MarketListItem)

    companion object {
        val mainCurrencies: List<String> =
            listOf("USD", "EUR", "GBP", "CAD", "AUD", "RUB", "CNY", "INR", "NGN")
    }
}