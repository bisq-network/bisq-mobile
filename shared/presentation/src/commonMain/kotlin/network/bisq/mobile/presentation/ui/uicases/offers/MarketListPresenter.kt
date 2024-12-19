package network.bisq.mobile.presentation.ui.uicases.offers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketFilter
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketSortBy
import network.bisq.mobile.presentation.ui.navigation.Routes

class MarketListPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter) {

    private var mainCurrencies = OfferbookServiceFacade.mainCurrencies

    private var _sortBy = MutableStateFlow(MarketSortBy.MostOffers)
    var sortBy: StateFlow<MarketSortBy> = _sortBy
    fun setSortBy(newValue: MarketSortBy) {
        _sortBy.value = newValue
    }

    private var _filter = MutableStateFlow(MarketFilter.WithOffers)
    var filter: StateFlow<MarketFilter> = _filter
    fun setFilter(newValue: MarketFilter) {
        _filter.value = newValue
    }

    private var _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText
    fun setSearchText(newValue: String) {
        _searchText.value = newValue
    }

    /*
    var marketListItems: List<MarketListItem> = offerbookServiceFacade.offerbookMarketItems
        .sortedWith(
            compareByDescending<MarketListItem> { it.numOffers.value }
                .thenByDescending { mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) } // [1]
                .thenBy { item ->
                    if (!mainCurrencies.contains(item.market.quoteCurrencyCode.lowercase())) item.market.quoteCurrencyName
                    else null // Null values will naturally be sorted together
                }
        )
        */
    // [1] thenBy doesn’t work as expected for boolean expressions because true and false are
    // sorted alphabetically (false before true), thus we use thenByDescending

    val marketListItemWithNumOffers: StateFlow<List<MarketListItem>> = combine(
            _filter,
            _searchText,
            _sortBy
        ) { filter: MarketFilter, searchText: String, sortBy: MarketSortBy ->
            computeMarketList(filter, searchText, sortBy)
        }.stateIn(
            CoroutineScope(Dispatchers.Main),
            SharingStarted.Lazily,
            emptyList()
        )

    private fun computeMarketList(
        filter: MarketFilter,
        searchText: String,
        sortBy: MarketSortBy
    ): List<MarketListItem> {
        return offerbookServiceFacade.offerbookMarketItems
            .filter { item ->
                when (filter) {
                    MarketFilter.WithOffers -> item.numOffers.value > 0
                    MarketFilter.All -> true
                }
            }
            .filter { item ->
                searchText.isEmpty() ||
                    item.market.quoteCurrencyCode.contains(searchText, ignoreCase = true) ||
                    item.market.quoteCurrencyName.contains(searchText, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<MarketListItem> {
                    when (sortBy) {
                        MarketSortBy.MostOffers -> it.numOffers.value
                        else -> 0
                    }
                }
                    .thenByDescending { mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) }
                    .thenBy {
                        when (sortBy) {
                            MarketSortBy.NameAZ -> it.market.quoteCurrencyName
                            MarketSortBy.NameZA -> it.market.quoteCurrencyName
                            else -> null
                        }
                    }
                    .let { comparator ->
                        if (sortBy == MarketSortBy.NameZA) comparator.reversed() else comparator
                    }
            )
    }

    fun onSelectMarket(marketListItem: MarketListItem) {
        offerbookServiceFacade.selectMarket(marketListItem)
        rootNavigator.navigate(Routes.OfferList.name)
    }

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }
}
