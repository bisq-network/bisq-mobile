package network.bisq.mobile.presentation.tabs.offers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.presentation.common.ui.components.MarketCard
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.SearchWithFilterField
import network.bisq.mobile.presentation.common.ui.components.organisms.market.MarketFilters
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

@Composable
fun OfferbookMarketScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<OfferbookMarketPresenter>()

    val uiState by presenter.uiState.collectAsState()
    val searchText by presenter.searchText.collectAsState()

    OfferbookMarketScreenContent(
        uiState = uiState,
        searchText = searchText,
        onAction = presenter::onAction,
    )
}

@Composable
internal fun OfferbookMarketScreenContent(
    uiState: OfferbookMarketUiState,
    searchText: String,
    onAction: (OfferbookMarketUiAction) -> Unit,
) {
    var showFilterDialog by remember { mutableStateOf(false) }

    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.Top,
    ) {
        SearchWithFilterField(
            value = searchText,
            onValueChange = { onAction(OfferbookMarketUiAction.OnSearchTextChanged(it)) },
            isFilterActive = uiState.filter == MarketFilter.WithOffers,
            onFilterClick = { showFilterDialog = true },
        )

        BisqGap.V1()

        val listState = rememberLazyListState()

        // Scroll to top whenever filter, sort, or search criteria changes
        LaunchedEffect(uiState.filter, uiState.sortBy, searchText) {
            if (uiState.marketItems.isNotEmpty()) {
                listState.scrollToItem(0)
            }
        }

        LazyColumn(state = listState) {
            items(uiState.marketItems, key = { it.market.marketCodes }) { item ->
                MarketCard(
                    item = item,
                    hasIgnoredUsers = uiState.hasIgnoredUsers,
                    onClick = { onAction(OfferbookMarketUiAction.OnMarketSelected(item)) },
                )
            }
        }

        if (showFilterDialog) {
            BisqBottomSheet(onDismissRequest = { showFilterDialog = false }) {
                MarketFilters(
                    sortBy = uiState.sortBy,
                    filter = uiState.filter,
                    onSortByChange = { onAction(OfferbookMarketUiAction.OnSortByChanged(it)) },
                    onFilterChange = { onAction(OfferbookMarketUiAction.OnFilterChanged(it)) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun OfferbookMarketScreenContentPreview() {
    val mockMarketItems =
        listOf(
            MarketListItem(
                market = MarketVO(baseCurrencyCode = "BTC", quoteCurrencyCode = "USD"),
                localeFiatCurrencyName = "US Dollar",
                numOffers = 12,
            ),
            MarketListItem(
                market = MarketVO(baseCurrencyCode = "BTC", quoteCurrencyCode = "EUR"),
                localeFiatCurrencyName = "Euro",
                numOffers = 8,
            ),
            MarketListItem(
                market = MarketVO(baseCurrencyCode = "BTC", quoteCurrencyCode = "BRL"),
                localeFiatCurrencyName = "Brazilian Real",
                numOffers = 0,
            ),
        )
    BisqTheme.Preview {
        OfferbookMarketScreenContent(
            uiState =
                OfferbookMarketUiState(
                    hasIgnoredUsers = false,
                    filter = MarketFilter.All,
                    sortBy = MarketSortBy.MostOffers,
                    marketItems = mockMarketItems,
                ),
            searchText = "",
            onAction = {},
        )
    }
}
