package network.bisq.mobile.presentation.tabs.offers.usecase

import network.bisq.mobile.domain.data.model.MarketFilter
import network.bisq.mobile.domain.data.model.MarketSortBy
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.CurrencyUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.tabs.offers.MarketFilterUtil

class ComputeOfferbookMarketListUseCase(
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
) : Logging {
    operator fun invoke(
        filter: MarketFilter,
        searchText: String,
        sortBy: MarketSortBy,
        items: List<MarketListItem>,
    ): List<MarketListItem> {
        log.d { "Offerbook computing market list - input: ${items.size} markets, filter: $filter, search: '$searchText', sort: $sortBy" }

        val translatedMarketItems =
            items.map { item ->
                item.copy(
                    localeFiatCurrencyName =
                        CurrencyUtils.getLocaleFiatCurrencyName(
                            item.market.quoteCurrencyCode,
                            item.market.quoteCurrencyName,
                        ),
                )
            }

        val marketsWithPriceData =
            MarketFilterUtil.filterMarketsWithPriceData(translatedMarketItems, marketPriceServiceFacade)
        log.d { "Offerbook after price filtering: ${marketsWithPriceData.size}/${translatedMarketItems.size} markets have price data" }

        val afterOfferFilter =
            marketsWithPriceData.filter { item ->
                when (filter) {
                    MarketFilter.WithOffers -> item.numOffers > 0
                    MarketFilter.All -> true
                }
            }
        log.d { "Offerbook after offer filtering ($filter): ${afterOfferFilter.size}/${marketsWithPriceData.size} markets" }

        val afterSearchFilter = MarketFilterUtil.filterMarketsBySearch(afterOfferFilter, searchText)
        if (searchText.isNotBlank()) {
            log.d { "Offerbook after search filtering ('$searchText'): ${afterSearchFilter.size}/${afterOfferFilter.size} markets" }
        }

        return afterSearchFilter.sortedWith(
            when (sortBy) {
                MarketSortBy.MostOffers -> compareByDescending<MarketListItem> { it.numOffers }.thenBy { it.localeFiatCurrencyName }
                MarketSortBy.NameAZ -> compareBy { it.localeFiatCurrencyName }
                MarketSortBy.NameZA -> compareByDescending { it.localeFiatCurrencyName }
            },
        )
    }
}
