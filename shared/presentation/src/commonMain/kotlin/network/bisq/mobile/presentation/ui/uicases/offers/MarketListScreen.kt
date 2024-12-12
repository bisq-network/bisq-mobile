package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.CurrencyProfileCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun MarketListScreen() {
    val strings = LocalStrings.current.common
    val presenter: MarketListPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    BisqStaticLayout(padding = PaddingValues(all = 0.dp), verticalArrangement = Arrangement.Top) {
        BisqTextField(label = "", placeholder = strings.common_search, value ="", onValueChanged = {})

        BisqGap.V1()

        LazyColumn {
            items(presenter.marketListItemWithNumOffers) { item ->
                CurrencyProfileCard(
                    item,
                    onClick = { presenter.onSelectMarket(item) }
                )
            }
        }
    }
}