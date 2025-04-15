package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    companion object {
        const val FILTER_REFRESH_FREQUENCY = 3000L
    }

    //todo
    //var marketListItemWithNumOffers: List<MarketListItem> = offerbookServiceFacade.getSortedOfferbookMarketItems()

    private val jobScope = CoroutineScope(SupervisorJob())
    private var updateJob: Job? = null

    private var mainCurrencies = OffersServiceFacade.mainCurrencies

    // flag to force market update trigger when needed
    private val _marketPriceUpdated = MutableStateFlow(false)

    //TODO not used
    var marketPriceUpdated: StateFlow<Boolean> = _marketPriceUpdated

    private val _sortBy = MutableStateFlow(MarketSortBy.MostOffers)
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
        _sortBy,
        _marketPriceUpdated
    ) { filter: MarketFilter, searchText: String, sortBy: MarketSortBy, forceTrigger: Boolean ->
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
        startUpdatingMarketPrices()
    }

    override fun onViewUnattaching() {
        stopUpdatingMarketPrices()
        super.onViewUnattaching()
    }

    private fun startUpdatingMarketPrices() {
        // Launch a coroutine that updates the market prices every 3 seconds
        updateJob = jobScope.launch {
            while (updateJob != null) { // Ensure the loop stops when the coroutine is cancelled
                updateMarketPrices()
                delay(FILTER_REFRESH_FREQUENCY) // Wait for 3 seconds before the next update
            }
        }
    }

    private fun stopUpdatingMarketPrices() {
        // Cancel the job to stop the background updates
        updateJob?.cancel()
        updateJob = null
    }

    private fun updateMarketPrices() {
        // fake trigger a refresh
        _marketPriceUpdated.value = !_marketPriceUpdated.value
    }
}
