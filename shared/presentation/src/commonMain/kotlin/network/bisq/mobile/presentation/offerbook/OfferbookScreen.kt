package network.bisq.mobile.presentation.offerbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiState
import network.bisq.mobile.presentation.common.ui.alert.dialog.TradeRestrictedDialog
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.BisqFABAddButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

@ExcludeFromCoverage
@Composable
fun OfferbookScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<OfferbookPresenter>()

    val sortedFilteredOffers by presenter.sortedFilteredOffers.collectAsState()
    val selectedDirection by presenter.selectedDirection.collectAsState()
    val showDeleteConfirmation by presenter.showDeleteConfirmation.collectAsState()
    val showNotEnoughReputationDialog by presenter.showNotEnoughReputationDialog.collectAsState()
    val showTradeRestrictedDialog by presenter.showTradeRestrictedDialog.collectAsState()
    val isCreateOfferEnabled by presenter.isCreateOfferEnabled.collectAsState()
    val isDeleteOfferEnabled by presenter.isDeleteOfferEnabled.collectAsState()
    val isTakeOfferEnabled by presenter.isTakeOfferEnabled.collectAsState()
    val selectedMarket by presenter.selectedMarket.collectAsState()
    val showLoading by presenter.isLoading.collectAsState()
    val filterUiState by presenter.filterUiState.collectAsState()

    OfferbookContent(
        sortedFilteredOffers = sortedFilteredOffers,
        selectedDirection = selectedDirection,
        selectedMarket = selectedMarket,
        filterUiState = filterUiState,
        showLoading = showLoading,
        showDeleteConfirmation = showDeleteConfirmation,
        showNotEnoughReputationDialog = showNotEnoughReputationDialog,
        showTradeRestrictedDialog = showTradeRestrictedDialog,
        isCreateOfferEnabled = isCreateOfferEnabled,
        isDeleteOfferEnabled = isDeleteOfferEnabled,
        isTakeOfferEnabled = isTakeOfferEnabled,
        isDemo = presenter.isDemo(),
        userProfileIconProvider = presenter.userProfileIconProvider,
        isReputationWarningForSellerAsTaker = presenter.isReputationWarningForSellerAsTaker,
        notEnoughReputationHeadline = presenter.notEnoughReputationHeadline,
        notEnoughReputationMessage = presenter.notEnoughReputationMessage,
        onSelectDirection = presenter::onSelectDirection,
        onCreateOffer = presenter::createOffer,
        onOfferSelect = presenter::onOfferSelected,
        onTogglePayment = presenter::togglePaymentMethod,
        onToggleSettlement = presenter::toggleSettlementMethod,
        onOnlyMyOffersChange = presenter::setOnlyMyOffers,
        onClearAllFilters = presenter::clearAllFilters,
        onSetPaymentSelection = presenter::setPaymentSelection,
        onSetSettlementSelection = presenter::setSettlementSelection,
        onConfirmDeleteOffer = presenter::onConfirmedDeleteOffer,
        onDismissDeleteOffer = presenter::onDismissDeleteOffer,
        onNavigateToReputation = presenter::onNavigateToReputation,
        onOpenReputationWiki = presenter::onOpenReputationWiki,
        onDismissNotEnoughReputationDialog = presenter::onDismissNotEnoughReputationDialog,
        onTradeRestrictingAlertAction = presenter::onTradeRestrictingAlertAction,
    )
}

@Composable
internal fun OfferbookContent(
    sortedFilteredOffers: List<OfferItemPresentationModel>,
    selectedDirection: DirectionEnum,
    selectedMarket: MarketPriceItem?,
    filterUiState: OfferbookFilterUiState,
    showLoading: Boolean,
    showDeleteConfirmation: Boolean,
    showNotEnoughReputationDialog: Boolean,
    showTradeRestrictedDialog: AlertNotificationUiState?,
    isCreateOfferEnabled: Boolean,
    isDeleteOfferEnabled: Boolean,
    isTakeOfferEnabled: Boolean,
    isDemo: Boolean,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    isReputationWarningForSellerAsTaker: Boolean,
    notEnoughReputationHeadline: String,
    notEnoughReputationMessage: String,
    onSelectDirection: (DirectionEnum) -> Unit,
    onCreateOffer: () -> Unit,
    onOfferSelect: (OfferItemPresentationModel) -> Unit,
    onTogglePayment: (String) -> Unit,
    onToggleSettlement: (String) -> Unit,
    onOnlyMyOffersChange: (Boolean) -> Unit,
    onClearAllFilters: () -> Unit,
    onSetPaymentSelection: (Set<String>) -> Unit,
    onSetSettlementSelection: (Set<String>) -> Unit,
    onConfirmDeleteOffer: () -> Unit,
    onDismissDeleteOffer: () -> Unit,
    onNavigateToReputation: () -> Unit,
    onOpenReputationWiki: () -> Unit,
    onDismissNotEnoughReputationDialog: () -> Unit,
    onTradeRestrictingAlertAction: (AlertNotificationUiAction) -> Unit,
) {
    val isOfferSelectionEnabled = isDeleteOfferEnabled && isTakeOfferEnabled

    BisqStaticScaffold(
        topBar = {
            val quoteCode =
                selectedMarket
                    ?.market
                    ?.quoteCurrencyCode
                    ?.takeIf { it.isNotBlank() }
                    ?.uppercase()
            TopBar(title = "mobile.offerbook.title".i18n(quoteCode ?: "—"))
        },
        floatingButton = {
            BisqFABAddButton(
                onClick = onCreateOffer,
                enabled = !isDemo && isCreateOfferEnabled,
            )
        },
        shouldBlurBg = showDeleteConfirmation || showNotEnoughReputationDialog || showTradeRestrictedDialog != null,
    ) {
        DirectionToggle(
            selectedDirection,
            onStateChange = onSelectDirection,
        )

        // Track bottom sheet expansion at the screen level to avoid auto-closing when we temporarily hide.
        var filterExpanded by remember { mutableStateOf(false) }

        // Hide the filter controller when there are no offers and no filters are active,
        // but keep it visible if the bottom sheet is currently expanded.
        val shouldShowFilter = filterUiState.hasActiveFilters || sortedFilteredOffers.isNotEmpty() || filterExpanded
        if (shouldShowFilter) {
            BisqGap.V1()
            OfferbookFilterController(
                state = filterUiState,
                onTogglePayment = onTogglePayment,
                onToggleSettlement = onToggleSettlement,
                onOnlyMyOffersChange = onOnlyMyOffersChange,
                onClearAll = onClearAllFilters,
                onSetPaymentSelection = onSetPaymentSelection,
                onSetSettlementSelection = onSetSettlementSelection,
                isExpanded = filterExpanded,
                onExpandedChange = { filterExpanded = it },
            )
        }

        if (sortedFilteredOffers.isEmpty() && !showLoading) {
            NoOffersSection(onCreateOffer = onCreateOffer)
            return@BisqStaticScaffold
        }

        BisqGap.V1()

        // Vertical edge fades on the offers list to hint scrollability
        val listState = rememberLazyListState()
        val canScrollUp by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }
        val canScrollDown by remember {
            derivedStateOf {
                val info = listState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()
                last != null && (last.index < info.totalItemsCount - 1 || (last.offset + last.size) > info.viewportEndOffset)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = BisqUIConstants.ScreenPadding5X),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(items = sortedFilteredOffers, key = { it.offerId }) { item ->
                    OfferCard(
                        item,
                        onSelectOffer = {
                            onOfferSelect(item)
                        },
                        userProfileIconProvider = userProfileIconProvider,
                        enabled = isOfferSelectionEnabled,
                    )
                }
            }

            // Subtle edge fades to indicate vertical scrollability (only when list has content)
            val fadeHeight = 12.dp
            if (sortedFilteredOffers.isNotEmpty() && canScrollUp) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(fadeHeight)
                            .align(Alignment.TopCenter)
                            .background(
                                brush =
                                    verticalGradient(
                                        colors = listOf(BisqTheme.colors.dark_grey20, Color.Transparent),
                                    ),
                            ),
                )
            }
            if (sortedFilteredOffers.isNotEmpty() && canScrollDown) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(fadeHeight)
                            .align(Alignment.BottomCenter)
                            .background(
                                brush =
                                    verticalGradient(
                                        colors = listOf(Color.Transparent, BisqTheme.colors.dark_grey40),
                                    ),
                            ),
                )
            }
        }
        // Loading overlay for initial fetches on slow connections (e.g., Tor - specially on Connect)
        if (showLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BisqTheme.colors.primary)
            }
        }
    }

    if (showDeleteConfirmation) {
        ConfirmationDialog(
            headline = if (isDemo) "mobile.demo.action.disabled".i18n() else "bisqEasy.offerbook.chatMessage.deleteOffer.confirmation".i18n(),
            onConfirm = onConfirmDeleteOffer,
            onDismiss = { onDismissDeleteOffer() },
            confirmButtonLoading = !isDeleteOfferEnabled,
        )
    }

    if (showNotEnoughReputationDialog) {
        if (isReputationWarningForSellerAsTaker) {
            ConfirmationDialog(
                headline = notEnoughReputationHeadline,
                headlineLeftIcon = { WarningIcon() },
                headlineColor = BisqTheme.colors.warning,
                message = notEnoughReputationMessage,
                confirmButtonText = "confirmation.yes".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                onConfirm = onNavigateToReputation,
                onDismiss = { onDismissNotEnoughReputationDialog() },
            )
        } else {
            WebLinkConfirmationDialog(
                link = BisqLinks.REPUTATION_WIKI_URL,
                headline = notEnoughReputationHeadline,
                headlineLeftIcon = { WarningIcon() },
                headlineColor = BisqTheme.colors.warning,
                message = notEnoughReputationMessage,
                confirmButtonText = "confirmation.yes".i18n(),
                dismissButtonText = "hyperlinks.openInBrowser.no".i18n(),
                onConfirm = onOpenReputationWiki,
                onDismiss = onDismissNotEnoughReputationDialog,
            )
        }
    }

    TradeRestrictedDialog(
        alert = showTradeRestrictedDialog,
        onAction = onTradeRestrictingAlertAction,
    )
}

@Composable
fun NoOffersSection(onCreateOffer: () -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding4X).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BisqText.H4LightGrey(
            text = "mobile.offerBookScreen.noOffersSection.thereAreNoOffers".i18n(), // There are no offers
            textAlign = TextAlign.Center,
        )
        BisqGap.V4()
        BisqButton(
            text = "offer.create".i18n(),
            onClick = onCreateOffer,
        )
    }
}
