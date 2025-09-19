package network.bisq.mobile.presentation.ui.uicases.open_trades

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.fiat_btc
import bisqapps.shared.presentation.generated.resources.reputation
import bisqapps.shared.presentation.generated.resources.thumbs_up
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.components.molecules.dialog.InformationConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun OpenTradeListScreen() {
    val presenter: OpenTradeListPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()

    val tradeGuideVisible by presenter.tradeGuideVisible.collectAsState()
    val tradeRulesConfirmed by presenter.tradeRulesConfirmed.collectAsState()
    val tradesWithUnreadMessages by presenter.tradesWithUnreadMessages.collectAsState()
    val sortedOpenTradeItems by presenter.sortedOpenTradeItems.collectAsState()

    val userAvatarMap by presenter.avatarMap.collectAsState()

    if (tradeGuideVisible) {
        InformationConfirmationDialog(
            message = "bisqEasy.tradeGuide.notConfirmed.warn".i18n(),
            confirmButtonText = "bisqEasy.tradeGuide.open".i18n(),
            dismissButtonText = "action.close".i18n(),
            onConfirm = {
                presenter.onCloseTradeGuideConfirmation()
                presenter.onOpenTradeGuide()
            },
            onDismiss = presenter::onCloseTradeGuideConfirmation
        )
    }

    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        isInteractive = isInteractive,
    ) {

        if (sortedOpenTradeItems.isEmpty()) {
            NoTradesSection(presenter)
        } else if (!tradeRulesConfirmed) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (tradeGuideVisible) Modifier.fillMaxSize()
                    .blur(8.dp) else Modifier.fillMaxSize()
            ) {
                item {
                    Column(
                        modifier = Modifier.clip(shape = RoundedCornerShape(12.dp))
                            .background(color = BisqTheme.colors.dark_grey30)
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
                        BisqText.h5Light("bisqEasy.openTrades.table.headline".i18n()) // My open trades
                        HorizontalDivider(
                            modifier = Modifier,
                            thickness = 0.5.dp,
                            color = BisqTheme.colors.mid_grey30
                        )
                    }
                }
                items(sortedOpenTradeItems, key = { it.tradeId }) { trade ->
                    OpenTradeListItem(
                        trade,
                        unreadCount = tradesWithUnreadMessages.getOrElse(trade.tradeId) { 0 },
                        userAvatar = userAvatarMap[trade.peersUserProfile.nym],
                        onSelect = { presenter.onSelect(trade) }
                    )
                }
            }
        //}
        } else {
            LazyColumn(
                // modifier = Modifier.padding(top = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                items(sortedOpenTradeItems, key = { it.tradeId }) { trade ->
                    OpenTradeListItem(
                        trade,
                        unreadCount = tradesWithUnreadMessages.getOrElse(trade.tradeId) { 0 },
                        userAvatar = userAvatarMap[trade.peersUserProfile.nym],
                        onSelect = { presenter.onSelect(trade) }
                    )
                }
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
            text = "bisqEasy.openTrades.welcome.headline".i18n(), // Welcome to your first Bisq Easy trade!
            textAlign = TextAlign.Center
        )
        BisqGap.VHalf()
        BisqText.baseLightGrey(
            // Please make yourself familiar with the concept, process and rules for trading on Bisq
            "bisqEasy.openTrades.welcome.info".i18n()
        )
        BisqGap.V1()
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            IconWithTextLine(
                image = Res.drawable.reputation,
                title = "bisqEasy.openTrades.welcome.line1".i18n() // Learn about the security model of Bisq easy
            )
            IconWithTextLine(
                image = Res.drawable.fiat_btc,
                title = "bisqEasy.openTrades.welcome.line2".i18n() // See how the trade process works
            )
            IconWithTextLine(
                image = Res.drawable.thumbs_up,
                title = "bisqEasy.openTrades.welcome.line3".i18n()  // Make yourself familiar with the trade rules
            )
        }
        BisqGap.V1()
        BisqButton(
            text = "bisqEasy.tradeGuide.open".i18n(), // Open trade guide
            onClick = presenter::onOpenTradeGuide
        )
    }
}

@Composable
fun IconWithTextLine(image: DrawableResource, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(image), null, Modifier.size(30.dp))
        Spacer(modifier = Modifier.width(15.dp))
        BisqText.baseLight(title)
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
                text = "bisqEasy.openTrades.noTrades".i18n(), // You don't have any open trades
                textAlign = TextAlign.Center
            )
            BisqButton(
                text = "mobile.bisqEasy.tradeWizard.selectOffer.noMatchingOffers.browseOfferbook".i18n(),
                onClick = presenter::onNavigateToOfferbook
            )
        }
    }
}