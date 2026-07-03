package network.bisq.mobile.presentation.tabs.offers

import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.data.model.offerbook.MarketListItem

data class OfferbookMarketUiState(
    val hasIgnoredUsers: Boolean = false,
    val filter: MarketFilter = Settings().marketFilter,
    val sortBy: MarketSortBy = Settings().marketSortBy,
    val marketItems: List<MarketListItem> = emptyList(),
)
