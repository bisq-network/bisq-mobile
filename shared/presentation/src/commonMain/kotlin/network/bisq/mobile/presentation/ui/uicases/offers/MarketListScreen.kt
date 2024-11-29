package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.CurrencyProfileCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun MarketListScreen() {
    val strings = LocalStrings.current
    val presenter: MarketListPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    BisqStaticLayout(
        verticalArrangement = Arrangement.Top,
        ) {
        BisqTextField(label = "", placeholder = strings.common_search, value ="", onValueChanged = {})

        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))

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