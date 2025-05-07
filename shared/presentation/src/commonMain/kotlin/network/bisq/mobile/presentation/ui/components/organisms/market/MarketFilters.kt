package network.bisq.mobile.presentation.ui.components.organisms.market

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.BisqSegmentButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookMarketPresenter
import org.koin.compose.koinInject

enum class MarketSortBy(val displayName: String) {
    MostOffers("Most Offers"),
    NameAZ("Name A-Z"),
    NameZA("Name Z-A")
}

enum class MarketFilter(val displayName: String) {
    WithOffers("With Offers"),
    All("All")
}


@Composable
fun MarketFilters() {

    val presenter: OfferbookMarketPresenter = koinInject()

    Column(modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding2X)) {

        BisqSegmentButton(
            label = "Sort by",
            value = presenter.sortBy.collectAsState().value.name,
            items = MarketSortBy.entries.map { it.name to it.displayName },
            onValueChange = {
                val newValue = when (it.second) {
                    MarketSortBy.MostOffers.displayName -> MarketSortBy.MostOffers
                    MarketSortBy.NameAZ.displayName -> MarketSortBy.NameAZ
                    else -> MarketSortBy.NameZA
                }
                presenter.setSortBy(newValue)
            },
        )

        BisqGap.V2()

        BisqSegmentButton(
            label = "Show markets",
            items = MarketFilter.entries.map { it.name to it.displayName },
            value = presenter.filter.collectAsState().value.name,
            onValueChange = {
                val newValue = when (it.second) {
                    MarketFilter.All.displayName -> MarketFilter.All
                    else -> MarketFilter.WithOffers
                }
                presenter.setFilter(newValue)
            },
        )

    }

}