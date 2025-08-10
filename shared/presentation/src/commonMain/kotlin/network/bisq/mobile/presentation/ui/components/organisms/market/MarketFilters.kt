package network.bisq.mobile.presentation.ui.components.organisms.market

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
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

fun MarketSortBy.getDisplayName(): String {
    return when (this) {
        MarketSortBy.MostOffers -> "mobile.components.marketFilter.sortBy.mostOffers".i18n()
        MarketSortBy.NameAZ -> "mobile.components.marketFilter.sortBy.nameAZ".i18n()
        MarketSortBy.NameZA -> "mobile.components.marketFilter.sortBy.nameZA".i18n()
    }
}

fun MarketFilter.getDisplayName(): String {
    return when (this) {
        MarketFilter.WithOffers -> "mobile.components.marketFilter.showMarkets.withOffers".i18n()
        MarketFilter.All -> "mobile.components.marketFilter.showMarkets.all".i18n()
    }
}


@Composable
fun MarketFilters() {

    val presenter: OfferbookMarketPresenter = koinInject()
    val sortBy by presenter.sortBy.collectAsState()
    val filter by presenter.filter.collectAsState()

    Column(modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding2X)) {

        BisqSegmentButton(
            label = "mobile.components.marketFilter.sortBy".i18n(),
            value = sortBy.name,
            items = MarketSortBy.entries.map { it.name to it.getDisplayName() },
            onValueChange = { pair ->
                presenter.setSortBy(MarketSortBy.valueOf(pair.first))
            },
        )

        BisqGap.V2()

        BisqSegmentButton(
            label = "mobile.components.marketFilter.showMarkets".i18n(),
            items = MarketFilter.entries.map { it.name to it.getDisplayName() },
            value = filter.name,
            onValueChange = { pair ->
                presenter.setFilter(MarketFilter.valueOf(pair.first))
            },
        )

    }

}