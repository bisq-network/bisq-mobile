package network.bisq.mobile.presentation.ui.uicases.offerbook

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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.presentation.ui.components.CurrencyCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.icons.GreenSortIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketFilter
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketFilters
import network.bisq.mobile.presentation.ui.components.organisms.market.MarketSortBy
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun OfferbookMarketScreen() {
    val presenter: OfferbookMarketPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    var showFilterDialog by remember { mutableStateOf(false) }
    val searchText by presenter.searchText.collectAsState()
    val isInteractive by presenter.isInteractive.collectAsState()
    val hasIgnoredUsers by presenter.hasIgnoredUsers.collectAsState()
    val marketItems by presenter.marketListItemWithNumOffers.collectAsState()
    val filter by presenter.filter.collectAsState()
    val sortBy by presenter.sortBy.collectAsState()

    OfferbookMarketScreenContent(
        searchText = searchText,
        isInteractive = isInteractive,
        hasIgnoredUsers = hasIgnoredUsers,
        marketItems = marketItems,
        filter = filter,
        sortBy = sortBy,
        showFilterDialog = showFilterDialog,
        onFilterClick = { showFilterDialog = true },
        onDismissFilterDialog = { showFilterDialog = false },
        onSearchTextChange = presenter::setSearchText,
        onMarketSelect = presenter::onSelectMarket,
        onSortByFilterChange = presenter::setSortBy,
        onFilterChange = presenter::setFilter
    )
}

@Composable
private fun OfferbookMarketScreenContent(
    searchText: String,
    isInteractive: Boolean,
    hasIgnoredUsers: Boolean,
    marketItems: List<MarketListItem>,
    filter: MarketFilter,
    sortBy: MarketSortBy,
    showFilterDialog: Boolean,
    onSortByFilterChange: (MarketSortBy) -> Unit,
    onFilterChange: (MarketFilter) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onMarketSelect: (MarketListItem) -> Unit,
    onDismissFilterDialog: () -> Unit,
) {
    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.Top,
        isInteractive = isInteractive,
    ) {
        BisqSearchField(
            value = searchText,
            onValueChanged = { text, _ -> onSearchTextChange(text) },
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
                    onClick = onFilterClick,
                    type = BisqButtonType.Clear,
                    modifier = Modifier.weight(1f),
                )
            }
        )

        BisqGap.V1()

        // Give the list a new identity when filter/sort changes so scroll resets to top
        key(filter, sortBy) {
            val listState = rememberLazyListState()
            val firstKey = marketItems.firstOrNull()?.market?.marketCodes
            // Scroll to top after the new sorted/filtered content is applied
            LaunchedEffect(firstKey) {
                if (firstKey != null) {
                    listState.scrollToItem(0)
                }
            }
            LazyColumn(state = listState) {
                items(marketItems, key = { it.market.marketCodes }) { item ->
                    CurrencyCard(
                        item = item,
                        hasIgnoredUsers = hasIgnoredUsers,
                        onClick = { onMarketSelect(item) }
                    )
                }
            }
        }

        if (showFilterDialog) {
            BisqBottomSheet(onDismissRequest = onDismissFilterDialog) {
                MarketFilters(
                    sortBy = sortBy,
                    filter = filter,
                    onSortByChange = onSortByFilterChange,
                    onFilterChange = onFilterChange
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
                numOffers = 12
            ),
            MarketListItem(
                market = MarketVO(baseCurrencyCode = "BTC", quoteCurrencyCode = "EUR"),
                localeFiatCurrencyName = "Euro",
                numOffers = 8
            ),
            MarketListItem(
                market = MarketVO(baseCurrencyCode = "BTC", quoteCurrencyCode = "BRL"),
                localeFiatCurrencyName = "Brazilian Real",
                numOffers = 0
            )
        )
    BisqTheme.Preview {
        OfferbookMarketScreenContent(
            searchText = "",
            isInteractive = true,
            hasIgnoredUsers = false,
            marketItems = mockMarketItems,
            filter = MarketFilter.All,
            sortBy = MarketSortBy.MostOffers,
            showFilterDialog = false,
            onSearchTextChange = {},
            onFilterClick = {},
            onMarketSelect = {},
            onDismissFilterDialog = {},
            onSortByFilterChange = {},
            onFilterChange = {}
        )
    }
}