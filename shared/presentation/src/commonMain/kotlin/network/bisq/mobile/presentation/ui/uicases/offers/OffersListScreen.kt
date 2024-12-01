package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.client.replicated_model.offer.Direction
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.DirectionToggle
import network.bisq.mobile.presentation.ui.components.molecules.OfferCard
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun OffersListScreen() {
    val presenter: OffersListPresenter = koinInject()
    val strings = LocalStrings.current.common

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
            TopBar(title = strings.common_offers)
        },
    ) {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
        DirectionToggle(
            offerDirections,
            presenter.selectedDirection.value,
            130.dp
        ) { direction ->
            presenter.onSelectDirection(direction)
        }

        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(sortedList) { item ->
                OfferCard(item, onClick = { presenter.takeOffer() })
            }
        }
    }
}

