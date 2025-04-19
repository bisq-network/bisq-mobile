package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqFABAddButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun OfferbookScreen() {
    val presenter: OfferbookPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    // Offers are mirrored to what user wants. E.g. I want to buy Bitcoin using a sell offer
    val offerDirections: List<DirectionEnum> = listOf(
        DirectionEnum.SELL,
        DirectionEnum.BUY
    )

    val sortedFilteredOffers by presenter.sortedFilteredOffers.collectAsState()
    val selectedDirection by presenter.selectedDirection.collectAsState()
    val showDeleteConfirmation by presenter.showDeleteConfirmation.collectAsState()
    val showNotEnoughReputationDialog by presenter.showNotEnoughReputationDialog.collectAsState()

    BisqStaticScaffold(
        topBar = {
            TopBar(title = "offers".i18n())
        },
        floatingButton = {
            BisqFABAddButton(
                onClick = { presenter.createOffer() },
                enabled = !presenter.isDemo()
            )
        }
    ) {
        DirectionToggle(
            offerDirections,
            selectedDirection,
            onStateChange = { direction -> presenter.onSelectDirection(direction) }
        )

        BisqGap.V1()

        if (showDeleteConfirmation) {
            ConfirmationDialog(
                headline = "bisqEasy.offerbook.chatMessage.deleteOffer.confirmation".i18n(),
                onConfirm = { presenter.onConfirmedDeleteOffer() },
                onDismiss = { presenter.onDismissDeleteOffer() }
            )
        }

        if (showNotEnoughReputationDialog) {
            WebLinkConfirmationDialog(
                link = "https://bisq.wiki/Reputation#How_to_build_reputation",
                headline = presenter.notEnoughReputationHeadline,
                message = presenter.notEnoughReputationMessage,
                confirmButtonText = "confirmation.yes".i18n(),
                dismissButtonText = "hyperlinks.openInBrowser.no".i18n(),
                onConfirm = { presenter.onLearnHowToBuildReputation() },
                onDismiss = { presenter.onDismissNotEnoughReputationDialog() }
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(sortedFilteredOffers) { item ->
                OfferCard(
                    item,
                    onSelectOffer = { presenter.onOfferSelected(item) },
                )
            }
        }
    }
}

