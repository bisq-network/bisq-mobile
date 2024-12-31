package network.bisq.mobile.presentation.ui.uicases.trade

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
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_no_trades
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.MockOffer
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.components.molecules.MyOfferCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

interface IMyTrades : ViewPresenter {
    val myTrades: StateFlow<List<MockOffer>>

    fun navigateToCurrencyList()

    fun createOffer()

    fun gotoTradeScreen(offer: MockOffer)
}

@Composable
fun MyTradesScreen() {
    val presenter: IMyTrades = koinInject()

    val myTrades: List<MockOffer> = presenter.myTrades.collectAsState().value

    RememberPresenterLifecycle(presenter)

    if (myTrades.isEmpty()) {
        NoTradesSection(presenter)
    } else {
        TradeList(presenter, myTrades)
    }

}


@Composable
fun TradeList(presenter: IMyTrades, myTrades: List<MockOffer>) {

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(myTrades) { offer ->
            MyOfferCard(
                offerListItem = offer,
                myTrade = true,
                onClick = { presenter.gotoTradeScreen(offer) },
                onChatClick = {},
            )
        }
    }

}

@Composable
fun NoTradesSection(presenter: IMyTrades) {
    BisqScrollLayout(verticalArrangement = Arrangement.Center) {
        Column(
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding2X),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painterResource(Res.drawable.img_no_trades), "",
                modifier = Modifier.height(220.dp).width(284.dp)
            )
            BisqGap.V2()
            BisqText.h3Regular(
                text = "A journey of a thousand miles begins with a first step!",
                color = BisqTheme.colors.light1,
                textAlign = TextAlign.Center
            )
            BisqGap.V4()
            BisqButton(
                text = "Start your first trade",
                onClick = { presenter.navigateToCurrencyList() },
            )
            BisqGap.V1()
            BisqButton(
                text = "Create a offer",
                onClick = { presenter.createOffer() }
            )
        }
    }
}