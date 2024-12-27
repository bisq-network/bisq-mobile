package network.bisq.mobile.domain.service.offerbook

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.OfferbookMarket
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO

interface OfferbookServiceFacade : LifeCycleAware {
    val offerbookMarketItems: List<MarketListItem>
    val offerListItems: StateFlow<List<OfferListItemVO>>
    val selectedOfferbookMarket: StateFlow<OfferbookMarket>

    fun selectOfferbookMarket(marketListItem: MarketListItem)

    fun getSortedOfferbookMarketItems(): List<MarketListItem> = offerbookMarketItems
        .sortedWith(
            compareByDescending<MarketListItem> { it.numOffers.value }
                .thenByDescending { mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) } // [1]
                .thenBy { item ->
                    if (!mainCurrencies.contains(item.market.quoteCurrencyCode.lowercase())) item.market.quoteCurrencyName
                    else null // Null values will naturally be sorted together
                }
        )
    // [1] thenBy doesn’t work as expected for boolean expressions because true and false are
    // sorted alphabetically (false before true), thus we use thenByDescending

    companion object {
        val mainCurrencies: List<String> =
            listOf("USD", "EUR", "GBP", "CAD", "AUD", "RUB", "CNY", "INR", "NGN")
    }
}