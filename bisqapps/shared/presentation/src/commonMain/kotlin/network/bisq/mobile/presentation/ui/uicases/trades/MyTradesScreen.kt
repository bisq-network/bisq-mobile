package network.bisq.mobile.presentation.ui.uicases.trades

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_no_trades
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.BisqOffer
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

interface IMyTrades : ViewPresenter {
    val myTrades: StateFlow<List<BisqOffer>>

    fun navigateToCurrencyList()
}

@Composable
fun MyTradesScreen() {
    val navController: NavHostController = koinInject(named("RootNavController"))
    val tabController: NavHostController = koinInject(named("TabNavController"))

    val presenter: IMyTrades = koinInject { parametersOf(navController, tabController) }

    val myTrades: List<BisqOffer> = presenter.myTrades.collectAsState().value

    RememberPresenterLifecycle(presenter)

    if (myTrades.isEmpty()) {
        NoTradesSection(presenter)
    } else {
        TradeList(presenter, myTrades)
    }

}


@Composable
fun TradeList(presenter: IMyTrades, myTrades: List<BisqOffer>) {

    LazyColumn(modifier = Modifier.padding(top = 48.dp)) {
        items(myTrades) { offer ->
            //OfferCard( onClick = {} )
        }
    }

}

@Composable
fun NoTradesSection(presenter: IMyTrades) {
    BisqScrollLayout(verticalArrangement = Arrangement.Center) {
        Column(
            modifier = Modifier.padding(vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(64.dp)
        ) {
            Image(
                painterResource(Res.drawable.img_no_trades), "",
                modifier = Modifier.height(272.dp).width(350.dp)
            )
            BisqText.h3Regular(
                text = "A journey of a thousand miles begins with a first step!",
                color = BisqTheme.colors.light1,
                textAlign = TextAlign.Center
            )
            BisqButton(
                text = "Start your first trade",
                onClick = { presenter.navigateToCurrencyList() }
            )
        }
    }
}