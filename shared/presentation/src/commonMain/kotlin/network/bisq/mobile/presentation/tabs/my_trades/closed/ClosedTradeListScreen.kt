package network.bisq.mobile.presentation.tabs.my_trades.closed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcome
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarPainters
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.GreenSortIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.rememberStarPainters
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileRow
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.tabs.my_trades.closed.components.ClosedTradeListFilterSheet
import network.bisq.mobile.presentation.tabs.my_trades.closed.components.TradeDetailsDialog
import network.bisq.mobile.presentation.tabs.my_trades.shared.TradeResultBar
import org.koin.compose.koinInject

@Composable
fun ClosedTradeListScreen() {
    val presenter: ClosedTradeListPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()
    val totalCount by presenter.totalCount.collectAsState()
    val lazyItems = presenter.pagingData.collectAsLazyPagingItems()
    val starPainters = rememberStarPainters()
    val isEmpty = lazyItems.itemCount == 0
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.sortBy, uiState.outcomeFilter, uiState.roleFilter, uiState.searchQuery) {
        snapshotFlow { lazyItems.loadState.refresh }
            .dropWhile { it !is LoadState.Loading }
            .first { it is LoadState.NotLoading }
        if (lazyItems.itemCount > 0) {
            listState.scrollToItem(0)
        }
    }

    uiState.selectedTradeForDetails?.let { item ->
        TradeDetailsDialog(
            item = item,
            onDismiss = { presenter.onAction(ClosedTradeListUiAction.OnDismissDetails) },
        )
    }

    if (uiState.showFilterSheet) {
        ClosedTradeListFilterSheet(
            sort = uiState.sortBy,
            outcome = uiState.outcomeFilter,
            role = uiState.roleFilter,
            onSortChange = { presenter.onAction(ClosedTradeListUiAction.OnSortChange(it)) },
            onOutcomeChange = { presenter.onAction(ClosedTradeListUiAction.OnOutcomeFilterChange(it)) },
            onRoleChange = { presenter.onAction(ClosedTradeListUiAction.OnRoleFilterChange(it)) },
            onReset = { presenter.onAction(ClosedTradeListUiAction.OnResetFilters) },
            onDismiss = { presenter.onAction(ClosedTradeListUiAction.OnDismissFilterSheet) },
        )
    }

    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.Top,
    ) {
        BisqGap.V1()

        BisqSearchField(
            value = uiState.searchQuery,
            onValueChange = { presenter.onAction(ClosedTradeListUiAction.OnSearchQueryChange(it)) },
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
                    onClick = { presenter.onAction(ClosedTradeListUiAction.OnShowFilterSheet) },
                    type = BisqButtonType.Clear,
                    modifier = Modifier.weight(1f),
                )
            },
        )

        TradeResultBar(
            sort = uiState.sortBy,
            outcome = uiState.outcomeFilter,
            role = uiState.roleFilter,
            loadedCount = lazyItems.itemCount,
            totalCount = totalCount ?: 0,
            onClearAll = { presenter.onAction(ClosedTradeListUiAction.OnResetFilters) },
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPaddingHalf),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = BisqUIConstants.ScreenPadding,
                    bottom = BisqUIConstants.ScreenPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            when {
                lazyItems.loadState.refresh is LoadState.Loading -> items(3, key = { "shimmer_$it" }) { ShimmerCard() }

                lazyItems.loadState.refresh is LoadState.Error ->
                    item(key = "error") {
                        ClosedTradeListErrorState(
                            modifier = Modifier.fillParentMaxSize(),
                            onRetry = { lazyItems.retry() },
                        )
                    }

                isEmpty && uiState.searchQuery.isBlank() && !uiState.isFilterActive ->
                    item(key = "empty") {
                        ClosedTradeListEmptyState(
                            modifier = Modifier.fillParentMaxSize(),
                            onBrowseOffers = { presenter.onAction(ClosedTradeListUiAction.OnBrowseOffers) },
                        )
                    }

                isEmpty ->
                    item(key = "no_results") {
                        ClosedTradeListNoResultsState(
                            modifier = Modifier.fillParentMaxSize(),
                            onClearSearch = { presenter.onAction(ClosedTradeListUiAction.OnClearSearch) },
                        )
                    }

                else -> {
                    items(
                        count = lazyItems.itemCount,
                        key = lazyItems.itemKey { it.tradeId },
                    ) { index ->
                        val item = lazyItems[index] ?: return@items
                        ClosedTradeListCard(
                            item = item,
                            userProfileIconProvider = presenter.userProfileIconProvider,
                            starPainters = starPainters,
                            onClick = { presenter.onAction(ClosedTradeListUiAction.OnSelectTrade(item)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosedTradeListCard(
    item: ClosedTradeListItem,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    starPainters: StarPainters,
    onClick: () -> Unit = {},
) {
    val borderColor = outcomeColor(item.outcome)
    val borderWidth = 4.dp
    val borderRadius = BisqUIConstants.BorderRadius
    val cardBackground = BisqTheme.colors.dark_grey30
    val shape =
        RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = borderRadius,
            bottomEnd = borderRadius,
        )

    val outcomeLabel =
        when (item.outcome) {
            TradeOutcome.COMPLETED -> "mobile.tradeHistory.outcome.completed".i18n()
            TradeOutcome.CANCELLED -> "mobile.tradeHistory.outcome.cancelled".i18n()
            TradeOutcome.REJECTED -> "mobile.tradeHistory.outcome.rejected".i18n()
            TradeOutcome.FAILED -> "mobile.tradeHistory.outcome.failed".i18n()
        }
    val fiatColor =
        if (item.outcome == TradeOutcome.COMPLETED) BisqTheme.colors.primary else BisqTheme.colors.mid_grey30
    val badgeBackground = borderColor.copy(alpha = 0.10f)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cardBackground)
                .drawBehind {
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = borderWidth.toPx(),
                    )
                }.clickable(onClick = onClick)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        // Outcome badge — tinted pill with 3dp colored left accent line + role label
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                    .background(badgeBackground)
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingQuarter,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 3.dp, height = 14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(borderColor),
                )
                BisqText.SmallRegular(text = outcomeLabel, color = borderColor)
            }
            BisqText.XSmallLight(text = item.myRoleWithDirection, color = BisqTheme.colors.mid_grey20)
        }

        BisqGap.VQuarter()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                BisqText.SmallLightGrey(item.directionalTitle.uppercase())
                UserProfileRow(
                    userProfile = item.peersUserProfile,
                    reputation = item.peersReputationScore,
                    userProfileIconProvider = userProfileIconProvider,
                    showUserName = true,
                    starPainters = starPainters,
                )
                BisqText.SmallLightGrey(item.formattedDateTime)
                BisqText.SmallLightGrey("Trade #${item.shortTradeId}")
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(top = 2.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                val formattedPrice = item.formattedPriceWithCode
                BisqText.LargeRegular(text = item.formattedQuoteAmountWithCode, color = fiatColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BisqText.SmallRegularGrey("@ ")
                    if (formattedPrice.length > 16) {
                        BisqText.XSmallRegular(formattedPrice)
                    } else {
                        BisqText.SmallRegular(formattedPrice)
                    }
                }
                BtcSatsText(item.formattedBaseAmount)
                PaymentMethods(
                    baseSidePaymentMethods = listOf(item.bitcoinSettlementMethod),
                    quoteSidePaymentMethods = listOf(item.fiatPaymentMethod),
                )
            }
        }
    }
}

@Composable
private fun outcomeColor(outcome: TradeOutcome): Color =
    when (outcome) {
        TradeOutcome.COMPLETED -> BisqTheme.colors.primary
        TradeOutcome.CANCELLED -> BisqTheme.colors.danger
        TradeOutcome.REJECTED -> BisqTheme.colors.danger
        TradeOutcome.FAILED -> BisqTheme.colors.warning
    }

@Composable
private fun ShimmerCard() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                    .background(BisqTheme.colors.dark_grey50),
        )
        BisqGap.VQuarter()
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.45f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BisqTheme.colors.dark_grey50),
        )
        Box(
            modifier =
                Modifier
                    .padding(start = BisqUIConstants.ScreenPadding6X)
                    .fillMaxWidth(0.40f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BisqTheme.colors.dark_grey50),
        )
        Box(
            modifier =
                Modifier
                    .padding(start = BisqUIConstants.ScreenPadding6X)
                    .fillMaxWidth(0.25f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BisqTheme.colors.dark_grey50),
        )
        BisqGap.V1()
        Box(
            modifier =
                Modifier
                    .align(Alignment.End)
                    .fillMaxWidth(0.40f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BisqTheme.colors.dark_grey50),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.30f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BisqTheme.colors.dark_grey50),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BisqTheme.colors.dark_grey50),
        )
    }
}

@Composable
private fun ClosedTradeListEmptyState(
    onBrowseOffers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding2X,
                    vertical = BisqUIConstants.ScreenPadding4X,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40),
            contentAlignment = Alignment.Center,
        ) { BisqText.H4LightGrey("?") }
        BisqGap.V2()
        BisqText.H5Light(
            text = "mobile.tradeHistory.empty.noTrades".i18n(),
            textAlign = TextAlign.Center,
        )
        BisqGap.V1()
        BisqText.SmallLightGrey(
            text = "mobile.tradeHistory.empty.noTrades.sub".i18n(),
            textAlign = TextAlign.Center,
        )
        BisqGap.V2()
        BisqButton(text = "action.browseOffers".i18n(), onClick = onBrowseOffers)
    }
}

@Composable
private fun ClosedTradeListNoResultsState(
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
private fun ClosedTradeListErrorState(
    onRetry: () -> Unit,
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
        BisqText.H5Light(
            text = "mobile.tradeHistory.error.title".i18n(),
            textAlign = TextAlign.Center,
        )
        BisqGap.V1()
        BisqText.SmallLightGrey(
            text = "mobile.tradeHistory.error.sub".i18n(),
            textAlign = TextAlign.Center,
        )
        BisqGap.V2()
        BisqButton(
            text = "mobile.tradeHistory.error.retry".i18n(),
            type = BisqButtonType.Grey,
            onClick = onRetry,
        )
    }
}

// -------------------------------------------------------------------------------------
// Preview samples
// -------------------------------------------------------------------------------------

private fun sampleClosedTrade(
    tradeId: String,
    peerName: String,
    reputation: ReputationScoreVO,
    fiatPaymentMethod: String,
    bitcoinSettlementMethod: String,
    isMaker: Boolean,
    isBuyer: Boolean,
    outcome: TradeOutcome,
    takeOfferDate: Long,
    quoteAmount: Long,
): ClosedTradeListItem =
    ClosedTradeListItem(
        tradeId = tradeId,
        peersUserProfile = createMockUserProfile(peerName),
        peersReputationScore = reputation,
        myUserProfile = createMockUserProfile("Me"),
        priceQuote = null,
        fiatPaymentMethod = fiatPaymentMethod,
        bitcoinSettlementMethod = bitcoinSettlementMethod,
        isMaker = isMaker,
        isBuyer = isBuyer,
        outcome = outcome,
        takeOfferDate = takeOfferDate,
        tradeCompletedDate = null,
        baseAmount = 0L,
        quoteAmount = quoteAmount,
        paymentAccountData = null,
        bitcoinPaymentData = null,
        paymentProof = null,
    )

private val sampleCompletedBuyerTrade =
    sampleClosedTrade(
        tradeId = "t-abc123def456ghi789",
        peerName = "SatoshiFan42",
        reputation = ReputationScoreVO(totalScore = 1200L, fiveSystemScore = 4.5, ranking = 12),
        fiatPaymentMethod = "SEPA",
        bitcoinSettlementMethod = "MAIN_CHAIN",
        isMaker = false,
        isBuyer = true,
        outcome = TradeOutcome.COMPLETED,
        takeOfferDate = 1_743_000_000_000L,
        quoteAmount = 34210L,
    )

private val sampleCompletedSellerTrade =
    sampleClosedTrade(
        tradeId = "t-xyz789abc123def456",
        peerName = "LightningLover99",
        reputation = ReputationScoreVO(totalScore = 800L, fiveSystemScore = 3.0, ranking = 45),
        fiatPaymentMethod = "REVOLUT",
        bitcoinSettlementMethod = "LN",
        isMaker = true,
        isBuyer = false,
        outcome = TradeOutcome.COMPLETED,
        takeOfferDate = 1_742_800_000_000L,
        quoteAmount = 82000L,
    )

private val sampleCancelledTrade =
    sampleClosedTrade(
        tradeId = "t-can000def456ghi789",
        peerName = "CryptoNovice7",
        reputation = ReputationScoreVO(totalScore = 200L, fiveSystemScore = 1.5, ranking = 200),
        fiatPaymentMethod = "ZELLE",
        bitcoinSettlementMethod = "MAIN_CHAIN",
        isMaker = false,
        isBuyer = true,
        outcome = TradeOutcome.CANCELLED,
        takeOfferDate = 1_742_700_000_000L,
        quoteAmount = 20000L,
    )

private val sampleRejectedTrade =
    sampleClosedTrade(
        tradeId = "t-rej001abc123def456",
        peerName = "FreshTrader",
        reputation = ReputationScoreVO(totalScore = 0L, fiveSystemScore = 0.0, ranking = 999),
        fiatPaymentMethod = "SEPA_INSTANT",
        bitcoinSettlementMethod = "LN",
        isMaker = false,
        isBuyer = false,
        outcome = TradeOutcome.REJECTED,
        takeOfferDate = 1_742_500_000_000L,
        quoteAmount = 50000L,
    )

private val sampleFailedTrade =
    sampleClosedTrade(
        tradeId = "t-fail002ghi789abc12",
        peerName = "NodeOp_Berlin",
        reputation = ReputationScoreVO(totalScore = 950L, fiveSystemScore = 4.0, ranking = 33),
        fiatPaymentMethod = "TWINT",
        bitcoinSettlementMethod = "MAIN_CHAIN",
        isMaker = true,
        isBuyer = true,
        outcome = TradeOutcome.FAILED,
        takeOfferDate = 1_742_300_000_000L,
        quoteAmount = 15000L,
    )

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Completed_Buyer_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ClosedTradeListCard(
                item = sampleCompletedBuyerTrade,
                userProfileIconProvider = { createEmptyImage() },
                starPainters = rememberStarPainters(),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Completed_Seller_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ClosedTradeListCard(
                item = sampleCompletedSellerTrade,
                userProfileIconProvider = { createEmptyImage() },
                starPainters = rememberStarPainters(),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Cancelled_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ClosedTradeListCard(
                item = sampleCancelledTrade,
                userProfileIconProvider = { createEmptyImage() },
                starPainters = rememberStarPainters(),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Rejected_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ClosedTradeListCard(
                item = sampleRejectedTrade,
                userProfileIconProvider = { createEmptyImage() },
                starPainters = rememberStarPainters(),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Failed_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ClosedTradeListCard(
                item = sampleFailedTrade,
                userProfileIconProvider = { createEmptyImage() },
                starPainters = rememberStarPainters(),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun List_MixedOutcomes_Preview() {
    val items =
        listOf(
            sampleCompletedBuyerTrade,
            sampleCompletedSellerTrade,
            sampleCancelledTrade,
            sampleRejectedTrade,
            sampleFailedTrade,
        )
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            items.forEach {
                ClosedTradeListCard(
                    item = it,
                    userProfileIconProvider = { createEmptyImage() },
                    starPainters = rememberStarPainters(),
                )
            }
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 500)
@Composable
private fun Shimmer_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            repeat(3) { ShimmerCard() }
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun EmptyState_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.fillMaxSize().background(BisqTheme.colors.backgroundColor)) {
            ClosedTradeListEmptyState(onBrowseOffers = {}, modifier = Modifier.fillMaxSize())
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 400)
@Composable
private fun NoResultsState_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.fillMaxSize().background(BisqTheme.colors.backgroundColor)) {
            ClosedTradeListNoResultsState(onClearSearch = {}, modifier = Modifier.fillMaxSize())
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 500)
@Composable
private fun ErrorState_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.fillMaxSize().background(BisqTheme.colors.backgroundColor)) {
            ClosedTradeListErrorState(onRetry = {}, modifier = Modifier.fillMaxSize())
        }
    }
}
