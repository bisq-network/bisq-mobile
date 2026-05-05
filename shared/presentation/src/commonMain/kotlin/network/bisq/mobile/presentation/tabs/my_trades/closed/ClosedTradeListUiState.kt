package network.bisq.mobile.presentation.tabs.my_trades.closed

import androidx.compose.runtime.Immutable
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

@Immutable
data class ClosedTradeListUiState(
    val searchQuery: String = "",
    val sortBy: TradeSort = TradeSort.NEWEST_FIRST,
    val outcomeFilter: TradeOutcomeFilter = TradeOutcomeFilter.ALL,
    val roleFilter: TradeRoleFilter = TradeRoleFilter.ALL,
    val showFilterSheet: Boolean = false,
    val selectedTradeForDetails: ClosedTradeListItem? = null,
) {
    val isFilterActive: Boolean
        get() =
            sortBy != TradeSort.NEWEST_FIRST ||
                outcomeFilter != TradeOutcomeFilter.ALL ||
                roleFilter != TradeRoleFilter.ALL
}
