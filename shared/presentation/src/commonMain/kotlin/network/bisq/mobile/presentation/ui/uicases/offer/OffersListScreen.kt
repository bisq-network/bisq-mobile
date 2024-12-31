package network.bisq.mobile.presentation.ui.uicases.offer

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
import network.bisq.mobile.domain.replicated.offer.DirectionEnum
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.DirectionToggle
import network.bisq.mobile.presentation.ui.components.molecules.OfferCard
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface IOffersListPresenter : ViewPresenter {
    val offerListItems: StateFlow<List<OfferListItemVO>>
    val selectedDirection: StateFlow<DirectionEnum>

    fun takeOffer(offer: OfferListItemVO)
    fun createOffer()
    fun chatForOffer(offer: OfferListItemVO)
    fun onSelectDirection(direction: DirectionEnum)
}

@Composable
fun OffersListScreen() {
    val commonStrings = LocalStrings.current.common
    val presenter: IOffersListPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    // Offers are mirrored to what user wants. E.g. I want to buy Bitcoin using a sell offer
    val offerDirections: List<DirectionEnum> = listOf(
        DirectionEnum.SELL,
        DirectionEnum.BUY
    )

    val offerListItems = presenter.offerListItems.collectAsState().value
    val selectedDirection = presenter.selectedDirection.collectAsState().value
    val filteredList = offerListItems.filter { it.bisqEasyOffer.direction == selectedDirection }
    val sortedList = filteredList.sortedByDescending { it.bisqEasyOffer.date }

    RememberPresenterLifecycle(presenter)

    BisqStaticScaffold(
        topBar = { TopBar(title = commonStrings.common_offers) },
        fab = {
            FloatingActionButton(
                onClick = { presenter.createOffer() },
                containerColor = BisqTheme.colors.primary,
                contentColor = BisqTheme.colors.light1,
            ) {
                AddIcon(modifier = Modifier.size(24.dp))
            }
        }
    ) {
        DirectionToggle(
            offerDirections,
            presenter.selectedDirection.value,
            130.dp
        ) { direction -> presenter.onSelectDirection(direction) }

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

