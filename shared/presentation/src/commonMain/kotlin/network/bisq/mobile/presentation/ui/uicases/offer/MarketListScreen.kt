package network.bisq.mobile.presentation.ui.uicases.offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.CurrencyCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqSearchField
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.components.molecules.BisqBottomSheet
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketFilters
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun MarketListScreen() {
    val strings = LocalStrings.current.common
    val presenter: MarketListPresenter = koinInject()
    var showFilterDialog by remember { mutableStateOf(false) }

    val marketItems = presenter.marketListItemWithNumOffers.collectAsState().value

    RememberPresenterLifecycle(presenter)

    BisqStaticLayout(padding = PaddingValues(all = 0.dp), verticalArrangement = Arrangement.Top) {

        BisqSearchField(
            value = presenter.searchText.collectAsState().value,
            onValueChanged = { presenter.setSearchText(it) },
            placeholder = strings.common_search,
            rightSuffix = {
                // TODO: Height to be reduced with Icon only buttons
                BisqButton(
                    iconOnly = { SortIcon() },
                    onClick = { showFilterDialog = true },
                    type = BisqButtonType.Clear
                )
            }
        )

        BisqGap.V1()

        LazyColumn {
            items(marketItems) { item ->
                CurrencyCard(
                    item,
                    onClick = { presenter.onSelectMarket(item) }
                )
            }
        }

        if (showFilterDialog) {
            BisqBottomSheet(onDismissRequest = { showFilterDialog = false }) {
                MarketFilters(
                    onConfirm = {},
                    onCancel = {}
                )
            }
        }
    }
}