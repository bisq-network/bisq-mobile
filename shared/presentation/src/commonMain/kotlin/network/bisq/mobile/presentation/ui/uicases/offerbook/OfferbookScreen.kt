package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqFABAddButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun OfferbookScreen() {
    val commonStrings = LocalStrings.current.common
    val presenter: OfferbookPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    // Offers are mirrored to what user wants. E.g. I want to buy Bitcoin using a sell offer
    val offerDirections: List<DirectionEnum> = listOf(
        DirectionEnum.SELL,
        DirectionEnum.BUY
    )

    val offerListItems = presenter.offerbookListItems.collectAsState().value
    val selectedDirection = presenter.selectedDirection.collectAsState().value
    val showDeleteConfirmationDialog = presenter.showDeleteConfirmation.collectAsState().value
    val filteredList = offerListItems.filter { it.bisqEasyOffer.direction.mirror == selectedDirection }
    val sortedList = filteredList.sortedByDescending { it.bisqEasyOffer.date }

    BisqStaticScaffold(
        topBar = {
            TopBar(title = commonStrings.common_offers)
        },
        fab = {
            BisqFABAddButton(
                onClick = { presenter.createOffer() },
            )
        }
    ) {
        DirectionToggle(
            offerDirections,
            selectedDirection,
            onStateChange = { direction -> presenter.onSelectDirection(direction) }
        )

        BisqGap.V1()

        if (showDeleteConfirmationDialog) {
            ConfirmationDialog(
                message = "bisqEasy.offerbook.chatMessage.deleteOffer.confirmation".i18n(),
//                subMessage = "You can resume later",
                onConfirm = {
                    presenter.proceedWithOfferAction()
                },
                onDismiss = {
                    presenter.onCancelDelete()
                }
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(sortedList) { item ->
                OfferCard(
                    item,
                    onSelectOffer = { presenter.onSelectOffer(item) },
                )
            }
        }
    }
}

