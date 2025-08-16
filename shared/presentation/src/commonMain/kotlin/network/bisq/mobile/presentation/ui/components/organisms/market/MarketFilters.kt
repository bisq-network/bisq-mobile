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

enum class MarketSortBy {
    MostOffers,
    NameAZ,
    NameZA,
}

enum class MarketFilter {
    WithOffers,
    All
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
            value = sortBy,
            items = MarketSortBy.entries.map { it to it.getDisplayName() },
            onValueChange = { pair ->
                presenter.setSortBy(pair.first)
            },
        )

        BisqGap.V2()

        BisqSegmentButton(
            label = "mobile.components.marketFilter.showMarkets".i18n(),
            items = MarketFilter.entries.map { it to it.getDisplayName() },
            value = filter,
            onValueChange = { pair ->
                presenter.setFilter(pair.first)
            },
        )

    }

}