package network.bisq.mobile.presentation.tabs.offers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.model.MarketFilter
import network.bisq.mobile.domain.data.model.MarketSortBy
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.offers.usecase.ComputeOfferbookMarketListUseCase

class OfferbookMarketPresenter(
    private val mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val computeOfferbookMarketListUseCase: ComputeOfferbookMarketListUseCase,
) : BasePresenter(mainPresenter) {
    // flag to force market update trigger when needed
    private val _marketPriceUpdated = MutableStateFlow(false)

    // Using MutableStateFlow instead of stateIn(presenterScope) so these survive
    // scope disposal on tab switch. Synced from DataStore in onViewAttached().
    // See https://github.com/bisq-network/bisq-mobile/issues/1197
    private val _hasIgnoredUsers = MutableStateFlow(false)
    val hasIgnoredUsers: StateFlow<Boolean> = _hasIgnoredUsers.asStateFlow()

    private val _sortBy = MutableStateFlow(Settings().marketSortBy)
    val sortBy: StateFlow<MarketSortBy> = _sortBy.asStateFlow()

    private val _filter = MutableStateFlow(Settings().marketFilter)
    val filter: StateFlow<MarketFilter> = _filter.asStateFlow()

    fun setSortBy(newValue: MarketSortBy) {
        presenterScope.launch {
            settingsRepository.setMarketSortBy(newValue)
        }
    }

    fun setFilter(newValue: MarketFilter) {
        presenterScope.launch {
            settingsRepository.setMarketFilter(newValue)
        }
    }

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> get() = _searchText.asStateFlow()

    fun setSearchText(newValue: String) {
        _searchText.value = newValue
    }

    private val _marketListItemWithNumOffers = MutableStateFlow<List<MarketListItem>>(emptyList())
    val marketListItemWithNumOffers: StateFlow<List<MarketListItem>> get() = _marketListItemWithNumOffers.asStateFlow()

    fun onSelectMarket(marketListItem: MarketListItem) {
        offersServiceFacade
            .selectOfferbookMarket(marketListItem)
            .onSuccess {
                navigateTo(NavRoute.Offerbook)
            }.onFailure { e ->
                log.e("Market selection failed", e)
                showSnackbar("Failed to select market. Please try again.", type = SnackbarType.ERROR)
            }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        syncSettingsFromDataStore()
        syncIgnoredUsers()
        observeMarketListItems()
        observeGlobalMarketPrices()
    }

    private fun syncSettingsFromDataStore() {
        presenterScope.launch {
            settingsRepository.data.collect { settings ->
                _filter.value = settings.marketFilter
                _sortBy.value = settings.marketSortBy
            }
        }
    }

    private fun syncIgnoredUsers() {
        presenterScope.launch {
            userProfileServiceFacade.ignoredProfileIds
                .map { it.isNotEmpty() }
                .collect { _hasIgnoredUsers.value = it }
        }
    }

    private fun observeMarketListItems() {
        presenterScope.launch {
            combine(
                filter,
                _searchText,
                sortBy,
                _marketPriceUpdated,
                mainPresenter.languageCode,
                offersServiceFacade.offerbookMarketItems,
            ) { values: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val filter = values[0] as MarketFilter
                val searchText = values[1] as String
                val sortBy = values[2] as MarketSortBy
                val items = values[5] as List<MarketListItem>
                computeOfferbookMarketListUseCase(filter, searchText, sortBy, items)
            }.collect { result ->
                _marketListItemWithNumOffers.value = result
            }
        }
    }

    private fun observeGlobalMarketPrices() {
        presenterScope.launch {
            marketPriceServiceFacade.globalPriceUpdate.collect { timestamp ->
                log.d { "Offerbook received global price update at timestamp: $timestamp" }
                val previousValue = _marketPriceUpdated.value
                _marketPriceUpdated.value = !_marketPriceUpdated.value
                log.d { "Offerbook triggered market filtering update: $previousValue -> ${_marketPriceUpdated.value}" }
            }
        }
    }
}
