package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.replicated_model.offer.Direction
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.DirectionToggle
import network.bisq.mobile.presentation.ui.components.molecules.OfferCard
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface IOffersListPresenter : ViewPresenter {
    val offerListItems: StateFlow<List<OfferListItem>>
    val selectedDirection: StateFlow<Direction>

    fun takeOffer(offer: OfferListItem)
    fun chatForOffer(offer: OfferListItem)
    fun onSelectDirection(direction: Direction)
}

@Composable
fun OffersListScreen() {
    val commonStrings = LocalStrings.current.common
    val presenter: IOffersListPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    // Offers are mirrored to what user wants. E.g. I want to buy Bitcoin using a sell offer
    val offerDirections: List<Direction> = listOf(
        Direction.SELL,
        Direction.BUY
    )

    val offerListItems = presenter.offerListItems.collectAsState().value
    val selectedDirection = presenter.selectedDirection.collectAsState().value
    val filteredList = offerListItems.filter { it.direction == selectedDirection }
    val sortedList = filteredList.sortedByDescending { it.date }

    RememberPresenterLifecycle(presenter)

    BisqStaticScaffold(
        topBar = {
            TopBar(title = commonStrings.common_offers)
        },
    ) {
        DirectionToggle(
            offerDirections,
            presenter.selectedDirection.value,
            130.dp
        ) { direction ->
            presenter.onSelectDirection(direction)
        }

        BisqGap.V1()

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(sortedList) { item ->
                OfferCard(
                    item,
                    onClick = { presenter.takeOffer(item) },
                            onChatClick = { presenter.chatForOffer(item) }
                )
            }
        }
    }
}

