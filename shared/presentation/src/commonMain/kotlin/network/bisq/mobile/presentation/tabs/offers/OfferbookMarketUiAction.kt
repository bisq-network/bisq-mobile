package network.bisq.mobile.presentation.tabs.offers

import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.data.model.offerbook.MarketListItem

sealed interface OfferbookMarketUiAction {
    data class OnSearchTextChanged(
        val searchText: String,
    ) : OfferbookMarketUiAction

    data class OnFilterChanged(
        val filter: MarketFilter,
    ) : OfferbookMarketUiAction

    data class OnSortByChanged(
        val sortBy: MarketSortBy,
    ) : OfferbookMarketUiAction

    data class OnMarketSelected(
        val marketListItem: MarketListItem,
    ) : OfferbookMarketUiAction
}
