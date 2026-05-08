package network.bisq.mobile.presentation.tabs.my_trades.closed

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.domain.usecase.trade.GetPaginatedClosedTradesUseCase
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.my_trades.closed.paging.ClosedTradesPagingSource

@OptIn(ExperimentalCoroutinesApi::class)
class ClosedTradeListPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val useCase: GetPaginatedClosedTradesUseCase,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {
    private companion object {
        const val FILTER_DEBOUNCE_MS = 400L
        const val CLOSED_TRADES_CHANGE_TICK_DEBOUNCE = 300L
    }

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    private data class QueryKey(
        val searchQuery: String,
        val sortBy: TradeSort,
        val outcomeFilter: TradeOutcomeFilter,
        val roleFilter: TradeRoleFilter,
        val refreshTick: Int,
    )

    private val _uiState = MutableStateFlow(ClosedTradeListUiState())
    val uiState: StateFlow<ClosedTradeListUiState> = _uiState.asStateFlow()

    private val _totalCount = MutableStateFlow<Int?>(null)
    val totalCount: StateFlow<Int?> = _totalCount.asStateFlow()

    private val refreshTick = MutableStateFlow(0)

    @OptIn(FlowPreview::class)
    val pagingData: Flow<PagingData<ClosedTradeListItem>> =
        run {
            val searchFlow =
                _uiState
                    .map { it.searchQuery }
                    .distinctUntilChanged()
                    .debounce { if (it.isEmpty()) 0L else 150L }
            val filtersFlow =
                _uiState
                    .map { Triple(it.sortBy, it.outcomeFilter, it.roleFilter) }
                    .distinctUntilChanged()
                    .debounce(FILTER_DEBOUNCE_MS)
            combine(searchFlow, filtersFlow, refreshTick) { query, filters, tick ->
                QueryKey(query, filters.first, filters.second, filters.third, tick)
            }.distinctUntilChanged()
                .onEach { _totalCount.value = null }
                .flatMapLatest { key ->
                    Pager(
                        config =
                            PagingConfig(
                                pageSize = PaginationParams.DEFAULT_PAGE_SIZE,
                                enablePlaceholders = false,
                            ),
                    ) {
                        ClosedTradesPagingSource(
                            useCase = useCase,
                            searchQuery = key.searchQuery,
                            sortBy = key.sortBy,
                            outcomeFilter = key.outcomeFilter,
                            roleFilter = key.roleFilter,
                            totalCountSink = _totalCount,
                        )
                    }.flow
                }.cachedIn(presenterScope)
        }

    @OptIn(FlowPreview::class)
    override fun onViewAttached() {
        super.onViewAttached()
        presenterScope.launch {
            tradesServiceFacade.closedTradesChangeTick
                .drop(1)
                .debounce(CLOSED_TRADES_CHANGE_TICK_DEBOUNCE)
                .collect { refreshTick.update { it + 1 } }
        }
    }

    fun onAction(action: ClosedTradeListUiAction) {
        when (action) {
            is ClosedTradeListUiAction.OnSearchQueryChange ->
                _uiState.update { it.copy(searchQuery = action.query) }

            is ClosedTradeListUiAction.OnSelectTrade ->
                _uiState.update { it.copy(selectedTradeForDetails = action.item) }

            ClosedTradeListUiAction.OnDismissDetails ->
                _uiState.update { it.copy(selectedTradeForDetails = null) }

            ClosedTradeListUiAction.OnShowFilterSheet -> _uiState.update { it.copy(showFilterSheet = true) }
            ClosedTradeListUiAction.OnDismissFilterSheet -> _uiState.update { it.copy(showFilterSheet = false) }

            is ClosedTradeListUiAction.OnSortChange ->
                _uiState.update { it.copy(sortBy = action.sort) }

            is ClosedTradeListUiAction.OnOutcomeFilterChange ->
                _uiState.update { it.copy(outcomeFilter = action.outcome) }

            is ClosedTradeListUiAction.OnRoleFilterChange ->
                _uiState.update { it.copy(roleFilter = action.role) }

            ClosedTradeListUiAction.OnResetFilters ->
                _uiState.update {
                    it.copy(
                        sortBy = TradeSort.NEWEST_FIRST,
                        outcomeFilter = TradeOutcomeFilter.ALL,
                        roleFilter = TradeRoleFilter.ALL,
                        showFilterSheet = false,
                    )
                }

            ClosedTradeListUiAction.OnClearSearch -> _uiState.update { it.copy(searchQuery = "") }
            ClosedTradeListUiAction.OnBrowseOffers -> navigateToTab(NavRoute.TabOfferbookMarket)
        }
    }
}
