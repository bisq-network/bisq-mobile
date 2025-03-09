package network.bisq.mobile.presentation.ui.uicases.open_trades

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.fiat_btc
import bisqapps.shared.presentation.generated.resources.reputation
import bisqapps.shared.presentation.generated.resources.thumbs_up
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun OpenTradeListScreen() {
    val presenter: OpenTradeListPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val sortedList = presenter.openTradeItems.collectAsState().value.sortedByDescending { it.bisqEasyTradeModel.takeOfferDate }

    if (presenter.tradeGuideVisible.collectAsState().value) {
        TradeGuide(presenter)
    }

    if (presenter.openTradeItems.collectAsState().value.isEmpty()) {
        NoTradesSection(presenter)
    } else if (!presenter.tradeRulesConfirmed.collectAsState().value) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (presenter.tradeGuideVisible.value) Modifier.fillMaxSize().blur(8.dp) else Modifier.fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier.clip(shape = RoundedCornerShape(12.dp))
                        .background(color = BisqTheme.colors.dark3)
                        .padding(12.dp),
                ) {
                    WelcomeToFirstTradePane(presenter)
                }
            }
            item {
                Column(
                    modifier = Modifier.clip(shape = RoundedCornerShape(8.dp))
                        // .background(color = BisqTheme.colors.dark3)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BisqText.h5Light("My open trades")
                    HorizontalDivider(
                        modifier = Modifier,
                        thickness = 0.5.dp,
                        color = BisqTheme.colors.grey3
                    )
                }
            }
            items(sortedList) { openTradeItem ->
                OpenTradeListItem(openTradeItem, { presenter.onSelect(openTradeItem) })
            }
        }
        //}
    } else {
        LazyColumn(
            // modifier = Modifier.padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(sortedList) { openTradeItem ->
                OpenTradeListItem(openTradeItem, { presenter.onSelect(openTradeItem) })
            }
        }
    }
}


@Composable
fun WelcomeToFirstTradePane(presenter: OpenTradeListPresenter) {
    Column(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BisqText.h1Light(
            text = "Welcome to your first Bisq Easy trade!", //bisqEasy.openTrades.welcome.headline
            textAlign = TextAlign.Center
        )
        BisqGap.VHalf()
        BisqText.baseRegularGrey(
            // bisqEasy.openTrades.welcome.info
            text = "Please make yourself familiar with the concept, process and rules for trading on Bisq Easy.\n" +
                    "After you have read and accepted the trade rules you can start the trade.",
        )
        BisqGap.V1()
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            IconWithTextLine(
                image = Res.drawable.reputation,
                title = "Learn about the security model of Bisq easy" //bisqEasy.openTrades.welcome.line1
            )
            IconWithTextLine(
                image = Res.drawable.fiat_btc,
                title = "See how the trade process works"  //bisqEasy.openTrades.welcome.line2
            )
            IconWithTextLine(
                image = Res.drawable.thumbs_up,
                title = "Make yourself familiar with the trade rules"  //bisqEasy.openTrades.welcome.line3
            )
        }
        BisqGap.V1()
        BisqButton(
            text = "Open trade guide",  //bisqEasy.tradeGuide.open
            onClick = {
                presenter.onOpenTradeGuide()
            }
        )
    }
}

@Composable
fun IconWithTextLine(image: DrawableResource, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(image), null, Modifier.size(30.dp))
        Spacer(modifier = Modifier.width(15.dp))
        BisqText.baseRegular(title)
    }
}

@Composable
fun NoTradesSection(presenter: OpenTradeListPresenter) {
    BisqScrollLayout(verticalArrangement = Arrangement.Center) {
        Column(
            modifier = Modifier.padding(vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(64.dp)
        ) {
            BisqText.h4LightGrey(
                text = "You don't have any open trades", // bisqEasy.openTrades.noTrades
                textAlign = TextAlign.Center
            )
            BisqButton(
                text = "Browse offerbook",
                onClick = { presenter.onNavigateToOfferbook() }
            )
        }
    }
}


// TODO Just for dev now. Will be custom screen with multiple sub screens...
@Composable
fun TradeGuide(presenter: OpenTradeListPresenter) {
    Dialog(
        onDismissRequest = { presenter.onCloseTradeGuide() } // Called when the user clicks outside the dialog

    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.0.dp),
            color = BisqTheme.colors.backgroundColor
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                BisqText.h3Light("Trade guide")
                BisqText.baseRegularGrey(text = "Trade guide content... TODO")
                BisqButton(
                    text = "Confirm trade rules",
                    onClick = { presenter.onConfirmTradeRules(true) },
                )
                BisqButton(
                    text = "Close",
                    onClick = { presenter.onCloseTradeGuide() },
                    backgroundColor = BisqTheme.colors.grey3,
                )
            }
        }
    }
}