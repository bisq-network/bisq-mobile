package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqFABAddButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun OfferbookScreen() {
    val presenter: OfferbookPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val sortedFilteredOffers by presenter.sortedFilteredOffers.collectAsState()
    val selectedDirection by presenter.selectedDirection.collectAsState()
    val showDeleteConfirmation by presenter.showDeleteConfirmation.collectAsState()
    val showNotEnoughReputationDialog by presenter.showNotEnoughReputationDialog.collectAsState()
    val isInteractive by presenter.isInteractive.collectAsState()
    val selectedMarket by presenter.selectedMarket.collectAsState()

    BisqStaticScaffold(
        topBar = {
            val quoteCode = selectedMarket?.market?.quoteCurrencyCode
                ?.takeIf { it.isNotBlank() }
                ?.uppercase()
            TopBar(title = "mobile.offerbook.title".i18n(quoteCode ?: "—"))
        },
        floatingButton = {
            BisqFABAddButton(
                onClick = { presenter.createOffer() },
                enabled = !presenter.isDemo()
            )
        },
        isInteractive = isInteractive,
        shouldBlurBg = showDeleteConfirmation || showNotEnoughReputationDialog
    ) {
        DirectionToggle(
            selectedDirection,
            onStateChange = { direction -> presenter.onSelectDirection(direction) }
        )

        // Phase 3: derive available payment/settlement methods from live offers
        fun paymentIconPath(id: String) = "drawable/payment/fiat/${id.lowercase().replace("-", "_")}.png"
        fun settlementIconPath(id: String) = when (id.uppercase()) {
            "BTC", "MAIN_CHAIN", "ONCHAIN", "ON_CHAIN" -> "drawable/payment/bitcoin/main_chain.png"
            "LIGHTNING", "LN" -> "drawable/payment/bitcoin/ln.png"
            else -> "drawable/payment/bitcoin/${id.lowercase().replace("-", "_")}.png"
        }

        val availablePaymentIds by presenter.availablePaymentMethodIds.collectAsState()
        val availableSettlementIds by presenter.availableSettlementMethodIds.collectAsState()

        // Keep selections stable across recompositions and offer set changes.
        var selectedPaymentIds by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet()) }
        var selectedSettlementIds by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet()) }
        var prevAvailPayment by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet()) }
        var prevAvailSettlement by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet()) }

        // Initialize defaults (all selected) and handle changes in available sets.
        if (prevAvailPayment != availablePaymentIds) {
            val newlyAdded = availablePaymentIds - prevAvailPayment
            selectedPaymentIds = (selectedPaymentIds intersect availablePaymentIds) + newlyAdded
            presenter.setSelectedPaymentMethodIds(selectedPaymentIds)
            prevAvailPayment = availablePaymentIds
        }
        if (prevAvailSettlement != availableSettlementIds) {
            val newlyAdded = availableSettlementIds - prevAvailSettlement
            selectedSettlementIds = (selectedSettlementIds intersect availableSettlementIds) + newlyAdded
            presenter.setSelectedSettlementMethodIds(selectedSettlementIds)
            prevAvailSettlement = availableSettlementIds
        }

        val paymentUi = availablePaymentIds.toList().sorted().map { id ->
            MethodIconState(id = id, label = id, iconPath = paymentIconPath(id), selected = id in selectedPaymentIds)
        }
        val settlementUi = availableSettlementIds.toList().sorted().map { id ->
            MethodIconState(id = id, label = id, iconPath = settlementIconPath(id), selected = id in selectedSettlementIds)
        }

        val hasActiveFilters = paymentUi.any { !it.selected } || settlementUi.any { !it.selected }
        val filterState = OfferbookFilterUiState(
            payment = paymentUi,
            settlement = settlementUi,
            onlyMyOffers = false,
            hasActiveFilters = hasActiveFilters,
        )

        BisqGap.V1()

        OfferbookFilterController(
            state = filterState,
            onTogglePayment = { id ->
                selectedPaymentIds = if (id in selectedPaymentIds) selectedPaymentIds - id else selectedPaymentIds + id
                presenter.setSelectedPaymentMethodIds(selectedPaymentIds)
            },
            onToggleSettlement = { id ->
                selectedSettlementIds = if (id in selectedSettlementIds) selectedSettlementIds - id else selectedSettlementIds + id
                presenter.setSelectedSettlementMethodIds(selectedSettlementIds)
            },
            onOnlyMyOffersChange = { /* Phase 7 */ },
            onClearAll = {
                selectedPaymentIds = availablePaymentIds
                selectedSettlementIds = availableSettlementIds
                presenter.setSelectedPaymentMethodIds(selectedPaymentIds)
                presenter.setSelectedSettlementMethodIds(selectedSettlementIds)
            },
            onSetPaymentSelection = { ids ->
                selectedPaymentIds = ids
                presenter.setSelectedPaymentMethodIds(selectedPaymentIds)
            },
            onSetSettlementSelection = { ids ->
                selectedSettlementIds = ids
                presenter.setSelectedSettlementMethodIds(selectedSettlementIds)
            },
        )


        if (sortedFilteredOffers.isEmpty()) {
            NoOffersSection(presenter)
            return@BisqStaticScaffold
        }

        BisqGap.V1()

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(items = sortedFilteredOffers, key = { it.offerId }) { item ->
                OfferCard(
                    item,
                    onSelectOffer = {
                        presenter.onOfferSelected(item)
                    },
                    userProfileIconProvider = presenter.userProfileIconProvider
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        ConfirmationDialog(
            headline = if (presenter.isDemo()) "Delete disabled on demo mode" else "bisqEasy.offerbook.chatMessage.deleteOffer.confirmation".i18n(),
            onConfirm = { presenter.onConfirmedDeleteOffer() },
            onDismiss = { presenter.onDismissDeleteOffer() }
        )
    }

    if (showNotEnoughReputationDialog) {
        if (presenter.isReputationWarningForSellerAsTaker) {
            ConfirmationDialog(
                headline = presenter.notEnoughReputationHeadline,
                headlineLeftIcon = { WarningIcon() },
                headlineColor = BisqTheme.colors.warning,
                message = presenter.notEnoughReputationMessage,
                confirmButtonText = "confirmation.yes".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                onConfirm = { presenter.onNavigateToReputation() },
                onDismiss = { presenter.onDismissNotEnoughReputationDialog() }
            )
        } else {
            WebLinkConfirmationDialog(
                link = BisqLinks.REPUTATION_WIKI_URL,
                headline = presenter.notEnoughReputationHeadline,
                headlineLeftIcon = { WarningIcon() },
                headlineColor = BisqTheme.colors.warning,
                message = presenter.notEnoughReputationMessage,
                confirmButtonText = "confirmation.yes".i18n(),
                dismissButtonText = "hyperlinks.openInBrowser.no".i18n(),
                onConfirm = { presenter.onOpenReputationWiki() },
                onDismiss = { presenter.onDismissNotEnoughReputationDialog() }
            )
        }
    }
}

@Composable
fun NoOffersSection(presenter: OfferbookPresenter) {
    Column(
        modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding4X).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BisqText.h4LightGrey(
            text = "mobile.offerBookScreen.noOffersSection.thereAreNoOffers".i18n(), // There are no offers
            textAlign = TextAlign.Center
        )
        BisqGap.V4()
        BisqButton(
            text = "offer.create".i18n(),
            onClick = presenter::createOffer
        )
    }
}