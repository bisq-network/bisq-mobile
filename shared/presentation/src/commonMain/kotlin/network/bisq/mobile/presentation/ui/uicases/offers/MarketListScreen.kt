package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.CurrencyProfileCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.BisqSearchField
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun MarketListScreen() {
    val strings = LocalStrings.current.common
    val presenter: MarketListPresenter = koinInject()
    var searchText by remember { mutableStateOf("") }

    RememberPresenterLifecycle(presenter)

    val filteredMarketItems = remember(searchText, presenter.marketListItemWithNumOffers) {
        if (searchText.isEmpty()) {
            presenter.marketListItemWithNumOffers
        } else {
            presenter.marketListItemWithNumOffers.filter { item ->
                item.market.quoteCurrencyCode.contains(searchText, ignoreCase = true) ||
                        item.market.quoteCurrencyName.contains(searchText, ignoreCase = true)
            }
        }
    }

    BisqStaticLayout(padding = PaddingValues(all = 0.dp), verticalArrangement = Arrangement.Top) {
        BisqSearchField(
            value = searchText,
            onValueChanged = { searchText = it },
            placeholder = strings.common_search,
//            rightSuffix = {
//                BisqButton(
//                    iconOnly = { SortIcon() },
//                    onClick = { println("Sort / filterrrr") }
//                )
//            }
        )

        BisqGap.V1()

        LazyColumn {
            items(filteredMarketItems) { item ->
                CurrencyProfileCard(
                    item,
                    onClick = { presenter.onSelectMarket(item) }
                )
            }
        }
    }
}