package network.bisq.mobile.presentation.tabs.my_trades.open

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.fiat_btc
import bisqapps.shared.presentation.generated.resources.reputation
import bisqapps.shared.presentation.generated.resources.thumbs_up
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.GreenSortIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.InformationConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.tabs.my_trades.open.components.OpenTradeListItem
import network.bisq.mobile.presentation.tabs.my_trades.open.components.OpenTradesFilterSheet
import network.bisq.mobile.presentation.tabs.my_trades.shared.TradeResultBar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun OpenTradeListScreen() {
    val presenter: OpenTradeListPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val tradeRulesConfirmed by presenter.tradeRulesConfirmed.collectAsState()
    val tradesWithUnreadMessages by presenter.tradesWithUnreadMessages.collectAsState()
    val uiState by presenter.uiState.collectAsState()

    val openTradesListState = rememberLazyListState()

    LaunchedEffect(uiState.sortBy, uiState.roleFilter, uiState.searchQuery) {
        snapshotFlow { uiState.filteredOpenTrades.isNotEmpty() }
            .first { it }
        openTradesListState.scrollToItem(0)
    }

    if (uiState.tradeGuideVisible) {
        InformationConfirmationDialog(
            message = "bisqEasy.tradeGuide.notConfirmed.warn".i18n(),
            confirmButtonText = "bisqEasy.tradeGuide.open".i18n(),
            dismissButtonText = "action.close".i18n(),
            onConfirm = {
                presenter.onAction(OpenTradeListUiAction.OnCloseTradeGuideConfirmation)
                presenter.onAction(OpenTradeListUiAction.OnOpenTradeGuide)
            },
            onDismiss = { presenter.onAction(OpenTradeListUiAction.OnCloseTradeGuideConfirmation) },
        )
    }

    if (uiState.showFilterSheet) {
        OpenTradesFilterSheet(
            sort = uiState.sortBy,
            role = uiState.roleFilter,
            onSortChange = { presenter.onAction(OpenTradeListUiAction.OnSortChange(it)) },
            onRoleChange = { presenter.onAction(OpenTradeListUiAction.OnRoleFilterChange(it)) },
            onReset = { presenter.onAction(OpenTradeListUiAction.OnResetFilters) },
            onDismiss = { presenter.onAction(OpenTradeListUiAction.OnDismissFilterSheet) },
        )
    }

    OpenTradeListContent(
        uiState = uiState,
        tradeRulesConfirmed = tradeRulesConfirmed,
        tradesWithUnreadMessages = tradesWithUnreadMessages,
        userProfileIconProvider = presenter.userProfileIconProvider,
        listState = openTradesListState,
        onAction = presenter::onAction,
    )
}

@Composable
private fun OpenTradeListContent(
    uiState: OpenTradeListUiState,
    tradeRulesConfirmed: Boolean,
    tradesWithUnreadMessages: Map<String, Int>,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    listState: LazyListState,
    onAction: (OpenTradeListUiAction) -> Unit,
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = BisqTheme.colors.primary,
                    strokeWidth = 2.dp,
                )
            }
        }

        else -> {
            if (uiState.totalCount == 0) {
                NoTradesSection(onNavigateToOfferbook = { onAction(OpenTradeListUiAction.OnNavigateToOfferbook) })
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    BisqGap.V1()
                    BisqSearchField(
                        value = uiState.searchQuery,
                        onValueChange = { onAction(OpenTradeListUiAction.OnSearchQueryChange(it)) },
                        placeholder = "mobile.tradeHistory.search.placeholder".i18n(),
                        rightSuffix = {
                            BisqButton(
                                iconOnly = {
                                    if (uiState.isFilterActive) {
                                        GreenSortIcon()
                                    } else {
                                        SortIcon()
                                    }
                                },
                                onClick = { onAction(OpenTradeListUiAction.OnShowFilterSheet) },
                                type = BisqButtonType.Clear,
                                modifier = Modifier.weight(1f),
                            )
                        },
                    )

                    TradeResultBar(
                        sort = uiState.sortBy,
                        role = uiState.roleFilter,
                        outcome = TradeOutcomeFilter.ALL,
                        loadedCount = uiState.filteredOpenTrades.size,
                        totalCount = uiState.totalCount,
                        onClearAll = { onAction(OpenTradeListUiAction.OnResetFilters) },
                        modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPaddingHalf),
                    )

                    OpenTradeListBody(
                        filteredOpenTrades = uiState.filteredOpenTrades,
                        searchQuery = uiState.searchQuery,
                        tradeRulesConfirmed = tradeRulesConfirmed,
                        tradeGuideVisible = uiState.tradeGuideVisible,
                        tradesWithUnreadMessages = tradesWithUnreadMessages,
                        userProfileIconProvider = userProfileIconProvider,
                        listState = listState,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenTradeListBody(
    filteredOpenTrades: List<TradeItemPresentationModel>,
    searchQuery: String,
    tradeRulesConfirmed: Boolean,
    tradeGuideVisible: Boolean,
    tradesWithUnreadMessages: Map<String, Int>,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    listState: LazyListState,
    onAction: (OpenTradeListUiAction) -> Unit,
) {
    if (filteredOpenTrades.isEmpty()) {
        OpenTradeListNoResultsState(
            modifier = Modifier.fillMaxSize(),
            onClearSearch = {
                onAction(
                    if (searchQuery.isNotBlank()) {
                        OpenTradeListUiAction.OnClearSearch
                    } else {
                        OpenTradeListUiAction.OnResetFilters
                    },
                )
            },
        )
        return
    }

    val listModifier =
        if (!tradeRulesConfirmed && tradeGuideVisible) {
            Modifier.fillMaxSize().blur(8.dp)
        } else {
            Modifier.fillMaxSize()
        }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = listModifier,
        contentPadding =
            PaddingValues(
                top = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.ScreenPadding,
            ),
    ) {
        if (!tradeRulesConfirmed) {
            item(key = "welcome") {
                Column(
                    modifier =
                        Modifier
                            .clip(shape = RoundedCornerShape(12.dp))
                            .background(color = BisqTheme.colors.dark_grey30)
                            .padding(12.dp),
                ) {
                    WelcomeToFirstTradePane(
                        onOpenTradeGuide = { onAction(OpenTradeListUiAction.OnOpenTradeGuide) },
                    )
                }
            }
        }

        items(filteredOpenTrades, key = { it.tradeId }) { trade ->
            OpenTradeListItem(
                trade,
                unreadCount = tradesWithUnreadMessages.getOrElse(trade.tradeId) { 0 },
                userProfileIconProvider = userProfileIconProvider,
                onSelect = { onAction(OpenTradeListUiAction.OnSelectTrade(trade)) },
            )
        }
    }
}

@Composable
private fun OpenTradeListNoResultsState(
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding2X,
                    vertical = BisqUIConstants.ScreenPadding3X,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        BisqGap.V2()
        BisqText.BaseLight(
            text = "mobile.tradeHistory.empty.noResults".i18n(),
            textAlign = TextAlign.Center,
        )
        BisqGap.V1()
        BisqButton(
            text = "action.clearSearch".i18n(),
            type = BisqButtonType.Grey,
            onClick = onClearSearch,
        )
    }
}

@Composable
fun WelcomeToFirstTradePane(onOpenTradeGuide: () -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BisqText.H1Light(
            text = "bisqEasy.openTrades.welcome.headline".i18n(),
            textAlign = TextAlign.Center,
        )
        BisqGap.VHalf()
        BisqText.BaseLightGrey(
            "bisqEasy.openTrades.welcome.info".i18n(),
        )
        BisqGap.V1()
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            IconWithTextLine(
                image = Res.drawable.reputation,
                title = "bisqEasy.openTrades.welcome.line1".i18n(),
            )
            IconWithTextLine(
                image = Res.drawable.fiat_btc,
                title = "bisqEasy.openTrades.welcome.line2".i18n(),
            )
            IconWithTextLine(
                image = Res.drawable.thumbs_up,
                title = "bisqEasy.openTrades.welcome.line3".i18n(),
            )
        }
        BisqGap.V1()
        BisqButton(
            text = "bisqEasy.tradeGuide.open".i18n(),
            onClick = onOpenTradeGuide,
        )
    }
}

@Composable
fun IconWithTextLine(
    image: DrawableResource,
    title: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(image), null, Modifier.size(30.dp))
        Spacer(modifier = Modifier.width(15.dp))
        BisqText.BaseLight(title)
    }
}

@Composable
fun NoTradesSection(onNavigateToOfferbook: () -> Unit) {
    BisqScrollLayout(verticalArrangement = Arrangement.Center) {
        Column(
            modifier = Modifier.padding(vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(64.dp),
        ) {
            BisqText.H4LightGrey(
                text = "bisqEasy.openTrades.noTrades".i18n(),
                textAlign = TextAlign.Center,
            )
            BisqButton(
                text = "mobile.bisqEasy.tradeWizard.selectOffer.noMatchingOffers.browseOfferbook".i18n(),
                onClick = onNavigateToOfferbook,
            )
        }
    }
}
