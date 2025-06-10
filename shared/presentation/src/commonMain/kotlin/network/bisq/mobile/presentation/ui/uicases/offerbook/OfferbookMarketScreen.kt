package network.bisq.mobile.presentation.ui.uicases.offerbook

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
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.CurrencyCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.icons.GreenSortIcon
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketFilter
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketFilters
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun OfferbookMarketScreen() {
    val presenter: OfferbookMarketPresenter = koinInject()
    var showFilterDialog by remember { mutableStateOf(false) }

    val marketItems by presenter.marketListItemWithNumOffers.collectAsState()
    val filter by presenter.filter.collectAsState()

    RememberPresenterLifecycle(presenter)

    BisqStaticLayout(
        padding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.Top,
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {

        BisqSearchField(
            value = presenter.searchText.collectAsState().value,
            onValueChanged = { it, isValid -> presenter.setSearchText(it) },
            rightSuffix = {
                // TODO: Height to be reduced with Icon only buttons
                BisqButton(
                    iconOnly = {
                        if (filter == MarketFilter.WithOffers) {
                            GreenSortIcon()
                        } else {
                            SortIcon()
                        }
                    },
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
                MarketFilters()
            }
        }
    }
}