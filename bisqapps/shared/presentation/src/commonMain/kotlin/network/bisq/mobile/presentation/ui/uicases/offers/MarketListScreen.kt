package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.presentation.ui.components.CurrencyProfileCard
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun CurrencyListScreen() {
    val strings = LocalStrings.current
    val presenter: MarketListPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    BisqStaticLayout(verticalArrangement = Arrangement.Top) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MaterialTextField(text = strings.common_search, onValueChanged = {})
            SortIcon(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))

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