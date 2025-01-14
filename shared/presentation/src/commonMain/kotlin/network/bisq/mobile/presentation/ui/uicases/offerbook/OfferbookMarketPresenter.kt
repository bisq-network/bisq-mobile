package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketFilter
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketSortBy
import network.bisq.mobile.presentation.ui.navigation.Routes

class OfferbookMarketPresenter(
    mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
) : BasePresenter(mainPresenter) {

    //todo
    //var marketListItemWithNumOffers: List<MarketListItem> = offerbookServiceFacade.getSortedOfferbookMarketItems()

    private var mainCurrencies = OffersServiceFacade.mainCurrencies

    private var _sortBy = MutableStateFlow(MarketSortBy.MostOffers)
    var sortBy: StateFlow<MarketSortBy> = _sortBy
    fun setSortBy(newValue: MarketSortBy) {
        _sortBy.value = newValue
    }

    private var _filter = MutableStateFlow(MarketFilter.All)
    var filter: StateFlow<MarketFilter> = _filter
    fun setFilter(newValue: MarketFilter) {
        _filter.value = newValue
    }

    private var _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText
    fun setSearchText(newValue: String) {
        _searchText.value = newValue
    }

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
        return offersServiceFacade.offerbookMarketItems
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
        offersServiceFacade.selectOfferbookMarket(marketListItem)
        navigateTo(Routes.OffersByMarket)
    }

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }
}
