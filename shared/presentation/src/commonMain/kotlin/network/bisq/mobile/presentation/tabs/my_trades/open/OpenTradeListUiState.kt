package network.bisq.mobile.presentation.tabs.my_trades.open

import androidx.compose.runtime.Immutable
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

@Immutable
data class OpenTradeListUiState(
    val searchQuery: String = "",
    val sortBy: TradeSort = TradeSort.NEWEST_FIRST,
    val roleFilter: TradeRoleFilter = TradeRoleFilter.ALL,
    val showFilterSheet: Boolean = false,
    val isLoading: Boolean = true,
    val tradeGuideVisible: Boolean = false,
    val totalCount: Int = 0,
    val filteredOpenTrades: List<TradeItemPresentationModel> = emptyList(),
) {
    val isFilterActive: Boolean
        get() = sortBy != TradeSort.NEWEST_FIRST || roleFilter != TradeRoleFilter.ALL
}
