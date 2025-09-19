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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqFABAddButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
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
    val userAvatarMap by presenter.avatarMap.collectAsState()
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
                    userAvatar = userAvatarMap[item.makersUserProfile.nym]
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
        WebLinkConfirmationDialog(
            link = BisqLinks.REPUTATION_BUILD_WIKI_URL,
            headline = presenter.notEnoughReputationHeadline,
            message = presenter.notEnoughReputationMessage,
            confirmButtonText = "confirmation.yes".i18n(),
            dismissButtonText = "hyperlinks.openInBrowser.no".i18n(),
            onConfirm = { presenter.onLearnHowToBuildReputation() },
            onDismiss = { presenter.onDismissNotEnoughReputationDialog() }
        )
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