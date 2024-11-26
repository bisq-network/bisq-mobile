package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.*
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface IOffersList : ViewPresenter {
    fun takeOffer()
}

@Composable
fun OffersListScreen() {
    val presenter: ICurrencyList = koinInject()
    val strings = LocalStrings.current

    val states = listOf(
        strings.offers_list_buy_from,
        strings.offers_list_sell_to
    )

    RememberPresenterLifecycle(presenter)

    BisqStaticScaffold(
        topBar = {
            TopBar(title = strings.common_offers)
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StateToggle(states, 130.dp)

                    Spacer(modifier = Modifier.height(32.dp))
                    LazyColumn(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(3) {
                            OfferCard(onClick = {
                                // TODO: Do navigation here
                            })
                        }
                    }

                }
            }
        }
    }
}

