package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun OpenTradeScreen() {
    val presenter: OpenTradePresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val tradeAbortedBoxVisible by presenter.tradeAbortedBoxVisible.collectAsState()
    val tradeProcessBoxVisible by presenter.tradeProcessBoxVisible.collectAsState()
    val isInMediation by presenter.isInMediation.collectAsState()
    val showCloseTradeDialog = false //presenter.showCloseTradeDialog.collectAsState().value

    BisqStaticScaffold(
        topBar = { TopBar("Trade ID: ${presenter.selectedTrade.value?.shortTradeId}") }
    ) {
        Box(modifier = Modifier.fillMaxSize().blur(if (showCloseTradeDialog) 12.dp else 0.dp)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                if (presenter.selectedTrade.value != null) {
                    TradeDetailsComposable()

                    if (isInMediation) {
                        BisqGap.V2()
                        Column(
                            modifier = Modifier.fillMaxSize()
                                .fillMaxWidth()
                                .clip(shape = RoundedCornerShape(12.dp))
                                .background(color = BisqTheme.colors.warning)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                // todo Add warn icon
                                BisqText.baseMedium(
                                    // bisqEasy.openTrades.inMediation.info
                                    text = "A mediator has joined the trade chat. Please use the trade chat below to get assistance from the mediator.",
                                    color = BisqTheme.colors.dark5 //todo maybe we can add a less dark grey as in bisq2
                                )
                            }
                        }
                    }

                    if (tradeAbortedBoxVisible) {
                        BisqGap.V2()
                        InterruptedTradePane()
                    }

                    if (tradeProcessBoxVisible) {
                        BisqGap.V2()
                        TradeFlowPane(presenter.tradeFlowPresenter)
                    }
                }

            }
            /*if (showCloseTradeDialog) {
                BisqDialog() {
                    CloseTradeCard(
                        onDismissRequest = {
                            presenter.setShowCloseTradeDialog(false)
                        },
                        onConfirm = {
                            presenter.closeTradeConfirm()
                        }
                    )
                }
            }*/
        }
    }
}
