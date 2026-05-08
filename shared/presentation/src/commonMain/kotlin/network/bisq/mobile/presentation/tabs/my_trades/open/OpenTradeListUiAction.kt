package network.bisq.mobile.presentation.tabs.my_trades.open

import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

sealed interface OpenTradeListUiAction {
    data class OnSearchQueryChange(
        val query: String,
    ) : OpenTradeListUiAction

    data object OnShowFilterSheet : OpenTradeListUiAction

    data object OnDismissFilterSheet : OpenTradeListUiAction

    data class OnSortChange(
        val sort: TradeSort,
    ) : OpenTradeListUiAction

    data class OnRoleFilterChange(
        val role: TradeRoleFilter,
    ) : OpenTradeListUiAction

    data object OnResetFilters : OpenTradeListUiAction

    data object OnClearSearch : OpenTradeListUiAction

    data object OnNavigateToOfferbook : OpenTradeListUiAction

    data object OnOpenTradeGuide : OpenTradeListUiAction

    data object OnCloseTradeGuideConfirmation : OpenTradeListUiAction

    data class OnSelectTrade(
        val item: TradeItemPresentationModel,
    ) : OpenTradeListUiAction
}
