package network.bisq.mobile.presentation.tabs.my_trades.closed

import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

sealed interface ClosedTradeListUiAction {
    data class OnSearchQueryChange(
        val query: String,
    ) : ClosedTradeListUiAction

    data class OnSelectTrade(
        val item: ClosedTradeListItem,
    ) : ClosedTradeListUiAction

    data object OnDismissDetails : ClosedTradeListUiAction

    data object OnShowFilterSheet : ClosedTradeListUiAction

    data object OnDismissFilterSheet : ClosedTradeListUiAction

    data class OnSortChange(
        val sort: TradeSort,
    ) : ClosedTradeListUiAction

    data class OnOutcomeFilterChange(
        val outcome: TradeOutcomeFilter,
    ) : ClosedTradeListUiAction

    data class OnRoleFilterChange(
        val role: TradeRoleFilter,
    ) : ClosedTradeListUiAction

    data object OnResetFilters : ClosedTradeListUiAction

    data object OnClearSearch : ClosedTradeListUiAction

    data object OnBrowseOffers : ClosedTradeListUiAction
}
