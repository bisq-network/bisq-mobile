package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CurrencyUtils
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketFilter
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketSortBy
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.market.MarketFilterUtil

class OfferbookMarketPresenter(
    mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade
) : BasePresenter(mainPresenter) {

    private var mainCurrencies = OffersServiceFacade.mainCurrencies

    // flag to force market update trigger when needed
    private val _marketPriceUpdated = MutableStateFlow(false)

    val hasIgnoredUsers: StateFlow<Boolean> = userProfileServiceFacade.ignoredUserIds
        .map { it.isNotEmpty() }
        .stateIn(
            presenterScope,
            SharingStarted.Lazily,
            false,
        )

    private val _sortBy = MutableStateFlow(MarketSortBy.MostOffers)
    val sortBy: StateFlow<MarketSortBy> get() = _sortBy.asStateFlow()
    fun setSortBy(newValue: MarketSortBy) {
        _sortBy.value = newValue
    }

    private val _filter = MutableStateFlow(MarketFilter.All)
    val filter: StateFlow<MarketFilter> get() = _filter.asStateFlow()
    fun setFilter(newValue: MarketFilter) {
        _filter.value = newValue
    }

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> get() = _searchText.asStateFlow()
    fun setSearchText(newValue: String) {
        _searchText.value = newValue
    }

    val marketListItemWithNumOffers: StateFlow<List<MarketListItem>> = combine(
        _filter,
        _searchText,
        _sortBy,
        _marketPriceUpdated,
        mainPresenter.languageCode,
    ) { filter: MarketFilter, searchText: String, sortBy: MarketSortBy, forceTrigger: Boolean, _ ->
        Triple(filter, searchText, sortBy)
    }.combine(offersServiceFacade.offerbookMarketItems) { params, items ->
        computeMarketList(params.first, params.second, params.third, items)
    }.stateIn(
        presenterScope,
        SharingStarted.Lazily,
        emptyList()
    )

    private fun computeMarketList(
        filter: MarketFilter,
        searchText: String,
        sortBy: MarketSortBy,
        items: List<MarketListItem>,
    ): List<MarketListItem> {
        log.d { "Offerbook computing market list - input: ${items.size} markets, filter: $filter, search: '$searchText', sort: $sortBy" }

        val translatedMarketItems = items.map { item ->
            item.copy(localeFiatCurrencyName = CurrencyUtils.getLocaleFiatCurrencyName(item.market.quoteCurrencyCode, item.market.quoteCurrencyName))
        }

        val marketsWithPriceData = MarketFilterUtil.filterMarketsWithPriceData(translatedMarketItems, marketPriceServiceFacade)
        log.d { "Offerbook after price filtering: ${marketsWithPriceData.size}/${translatedMarketItems.size} markets have price data" }

        val afterOfferFilter = marketsWithPriceData.filter { item ->
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

        val finalResult = afterSearchFilter.sortedWith(
            compareByDescending<MarketListItem> {
                when (sortBy) {
                    MarketSortBy.MostOffers -> it.numOffers
                    else -> 0
                }
            }
                .thenByDescending { mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) }
                .thenBy {
                    when (sortBy) {
                        MarketSortBy.NameAZ -> it.localeFiatCurrencyName
                        MarketSortBy.NameZA -> it.localeFiatCurrencyName
                        else -> null
                    }
                }
                .let { comparator ->
                    if (sortBy == MarketSortBy.NameZA) comparator.reversed() else comparator
                }
        )
        return finalResult
    }

    fun onSelectMarket(marketListItem: MarketListItem) {
        offersServiceFacade.selectOfferbookMarket(marketListItem)
        navigateTo(Routes.OffersByMarket)
    }

    override fun onViewAttached() {
        super.onViewAttached()
        observeGlobalMarketPrices()
    }

    private fun observeGlobalMarketPrices() {
        collectIO(marketPriceServiceFacade.globalPriceUpdate) { timestamp ->
            log.d { "Offerbook received global price update at timestamp: $timestamp" }
            val previousValue = _marketPriceUpdated.value
            _marketPriceUpdated.value = !_marketPriceUpdated.value
            log.d { "Offerbook triggered market filtering update: $previousValue -> ${_marketPriceUpdated.value}" }
        }
    }
}
