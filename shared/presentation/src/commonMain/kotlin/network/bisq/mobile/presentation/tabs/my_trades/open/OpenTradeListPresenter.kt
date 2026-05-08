package network.bisq.mobile.presentation.tabs.my_trades.open

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.domain.usecase.trade.FilterOpenTradesUseCase
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

@OptIn(FlowPreview::class)
class OpenTradeListPresenter(
    private val mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val filterOpenTradesUseCase: FilterOpenTradesUseCase,
) : BasePresenter(mainPresenter) {
    private companion object {
        const val FILTER_DEBOUNCE_MS = 400L
    }

    private val _uiState = MutableStateFlow(OpenTradeListUiState())
    val uiState: StateFlow<OpenTradeListUiState> = _uiState.asStateFlow()

    val tradeRulesConfirmed: StateFlow<Boolean> get() = settingsServiceFacade.tradeRulesConfirmed
    val tradesWithUnreadMessages: StateFlow<Map<String, Int>> get() = mainPresenter.tradesWithUnreadMessages

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    override fun onViewAttached() {
        super.onViewAttached()
        tradesServiceFacade.resetSelectedTradeToNull()
        _uiState.update { it.copy(isLoading = true) }
        presenterScope.launch {
            val searchKey =
                _uiState
                    .map { it.searchQuery }
                    .distinctUntilChanged()

            val filterKey =
                _uiState
                    .map { it.sortBy to it.roleFilter }
                    .distinctUntilChanged()
                    .debounce(FILTER_DEBOUNCE_MS)

            combine(
                mainPresenter.tradesWithUnreadMessages,
                tradesServiceFacade.openTradeItems,
                mainPresenter.languageCode,
                searchKey,
                filterKey,
            ) { _, openTrades, _, query, (sort, role) ->
                openTrades to
                    filterOpenTradesUseCase.invoke(
                        items = openTrades,
                        searchQuery = query,
                        sortBy = sort,
                        roleFilter = role,
                    )
            }.flowOn(Dispatchers.Default)
                .collect { (all, filtered) ->
                    _uiState.update {
                        it.copy(
                            totalCount = all.size,
                            filteredOpenTrades = filtered,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun onAction(action: OpenTradeListUiAction) {
        when (action) {
            is OpenTradeListUiAction.OnSearchQueryChange -> onSearchQueryChange(action.query)
            OpenTradeListUiAction.OnShowFilterSheet -> onShowFilterSheet()
            OpenTradeListUiAction.OnDismissFilterSheet -> onDismissFilterSheet()
            is OpenTradeListUiAction.OnSortChange -> _uiState.update { it.copy(sortBy = action.sort) }
            is OpenTradeListUiAction.OnRoleFilterChange -> _uiState.update { it.copy(roleFilter = action.role) }
            OpenTradeListUiAction.OnResetFilters -> onResetFilters()
            OpenTradeListUiAction.OnClearSearch -> onClearSearch()
            OpenTradeListUiAction.OnNavigateToOfferbook -> onNavigateToOfferbook()
            OpenTradeListUiAction.OnOpenTradeGuide -> onOpenTradeGuide()
            OpenTradeListUiAction.OnCloseTradeGuideConfirmation -> onCloseTradeGuideConfirmation()
            is OpenTradeListUiAction.OnSelectTrade -> onSelectTrade(action.item)
        }
    }

    private fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun onShowFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = true) }
    }

    private fun onDismissFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = false) }
    }

    private fun onResetFilters() {
        _uiState.update {
            it.copy(
                sortBy = TradeSort.NEWEST_FIRST,
                roleFilter = TradeRoleFilter.ALL,
                showFilterSheet = false,
            )
        }
    }

    private fun onClearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    private fun onNavigateToOfferbook() {
        navigateToTab(NavRoute.TabOfferbookMarket)
    }

    private fun onOpenTradeGuide() {
        navigateTo(NavRoute.TradeGuideOverview)
    }

    private fun onCloseTradeGuideConfirmation() {
        _uiState.update { it.copy(tradeGuideVisible = false) }
    }

    private fun onSelectTrade(openTradeItem: TradeItemPresentationModel) {
        if (tradeRulesConfirmed.value) {
            navigateToOpenTrade(openTradeItem)
        } else {
            log.w { "User hasn't accepted trade rules yet, showing dialog" }
            _uiState.update { it.copy(tradeGuideVisible = true) }
        }
    }

    private fun navigateToOpenTrade(openTradeItem: TradeItemPresentationModel) {
        try {
            navigateTo(NavRoute.OpenTrade(openTradeItem.tradeId))
        } catch (e: Exception) {
            log.e(e) { "Failed to open trade ${openTradeItem.tradeId}" }
            showSnackbar("mobile.bisqEasy.openTrades.failed".i18n(e.message ?: "unknown"), type = SnackbarType.ERROR)
        }
    }
}
