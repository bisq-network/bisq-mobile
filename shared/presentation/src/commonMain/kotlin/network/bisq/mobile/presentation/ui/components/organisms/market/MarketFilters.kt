package network.bisq.mobile.presentation.ui.components.organisms.market

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.BisqDropDown
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
fun MarketFilters(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {

    val presenter: OfferbookMarketPresenter = koinInject()

    Column(modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding2X)) {

        BisqDropDown(
            label = "Sort by",
            value = presenter.sortBy.collectAsState().value.displayName,
            items = MarketSortBy.entries.map { it.displayName },
            onValueChanged = {
                val newValue = when (it) {
                    MarketSortBy.MostOffers.displayName -> MarketSortBy.MostOffers
                    MarketSortBy.NameAZ.displayName -> MarketSortBy.NameAZ
                    else -> MarketSortBy.NameZA
                }
                presenter.setSortBy(newValue)
            }
        )

        BisqGap.V2()

        BisqDropDown(
            label = "Show markets",
            value = presenter.filter.collectAsState().value.displayName,
            items = MarketFilter.entries.map { it.displayName },
            onValueChanged = {
                val newValue = when (it) {
                    MarketFilter.All.displayName -> MarketFilter.All
                    else -> MarketFilter.WithOffers
                }
                presenter.setFilter(newValue)            }
        )

//        BisqGap.V1()
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            BisqButton(
//                text = "Cancel",
//                onClick = onCancel,
//                backgroundColor = BisqTheme.colors.dark5,
//                padding = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
//            )
//            BisqButton(
//                text = "Apply",
//                onClick = onConfirm,
//                padding = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
//            )
//        }

    }

}