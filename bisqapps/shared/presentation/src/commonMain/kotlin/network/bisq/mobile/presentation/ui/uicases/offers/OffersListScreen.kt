package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.*
import org.koin.compose.koinInject

interface IOffersList : ViewPresenter {
    fun takeOffer()
}

@Composable
fun OffersListScreen() {
    val presenter: ICurrencyList = koinInject()
    val states = listOf(
        "Buy from",
        "Sell to"
    )
    val openDialog = remember { mutableStateOf(false) }
    val rootNavController: NavController

    LaunchedEffect(Unit) {
        presenter.onViewAttached()
    }

    BisqStaticScaffold(
        topBar = {
            TopBar(title = "Offers")
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().blur(if (openDialog.value) 12.dp else 0.dp)) {
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
                                openDialog.value = !openDialog.value
                            })
                        }
                    }

                    if (openDialog.value) {
                        ConfirmationDialog(
                            message = "Do you want to take this trade?",
                            confirmButtonText = "Yes, please",
                            cancelButtonText = "Cancel",
                            onDismissRequest = {
                                openDialog.value = !openDialog.value
                            },
                        )
                    }

                }
            }
        }
    }
}

