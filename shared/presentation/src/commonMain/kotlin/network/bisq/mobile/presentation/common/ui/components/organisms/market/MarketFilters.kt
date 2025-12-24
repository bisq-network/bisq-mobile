package network.bisq.mobile.presentation.common.ui.components.organisms.market

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSegmentButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class MarketSortBy {
    MostOffers,
    NameAZ,
    NameZA,
}

enum class MarketFilter {
    WithOffers,
    All,
}

fun MarketSortBy.getDisplayName(): String =
    when (this) {
        MarketSortBy.MostOffers -> "mobile.components.marketFilter.sortBy.mostOffers".i18n()
        MarketSortBy.NameAZ -> "mobile.components.marketFilter.sortBy.nameAZ".i18n()
        MarketSortBy.NameZA -> "mobile.components.marketFilter.sortBy.nameZA".i18n()
    }

fun MarketFilter.getDisplayName(): String =
    when (this) {
        MarketFilter.WithOffers -> "mobile.components.marketFilter.showMarkets.withOffers".i18n()
        MarketFilter.All -> "mobile.components.marketFilter.showMarkets.all".i18n()
    }

@Composable
fun MarketFilters(
    sortBy: MarketSortBy,
    filter: MarketFilter,
    onSortByChange: (MarketSortBy) -> Unit,
    onFilterChange: (MarketFilter) -> Unit,
) {
    Column(modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding2X)) {
        BisqSegmentButton(
            label = "mobile.components.marketFilter.sortBy".i18n(),
            value = sortBy,
            items = MarketSortBy.entries.map { it to it.getDisplayName() },
            onValueChange = { pair ->
                onSortByChange(pair.first)
            },
        )
        BisqGap.V2()
        BisqSegmentButton(
            label = "mobile.components.marketFilter.showMarkets".i18n(),
            items = MarketFilter.entries.map { it to it.getDisplayName() },
            value = filter,
            onValueChange = { pair ->
                onFilterChange(pair.first)
            },
        )
    }
}

@Preview
@Composable
private fun MarketFiltersPreview() {
    BisqTheme.Preview {
        MarketFilters(
            sortBy = MarketSortBy.MostOffers,
            filter = MarketFilter.WithOffers,
            onSortByChange = {},
            onFilterChange = {},
        )
    }
}
