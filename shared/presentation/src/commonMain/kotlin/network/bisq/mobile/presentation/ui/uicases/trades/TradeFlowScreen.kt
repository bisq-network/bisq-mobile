package network.bisq.mobile.presentation.ui.uicases.trades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.atoms.layout.*
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.organisms.trades.CloseTradeCard
import network.bisq.mobile.presentation.ui.components.organisms.trades.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface ITradeFlowPresenter : ViewPresenter {
    // TODO: Update later to refer to a single state specific object
    val offerListItems: StateFlow<List<OfferListItem>>

    val steps: List<TradeFlowScreenSteps>

    val receiveAddress: StateFlow<String>
    fun setReceiveAddress(value: String)

    val confirmingFiatPayment: StateFlow<Boolean>
    fun setConfirmingFiatPayment(value: Boolean)

    fun confirmFiatPayment()

    val showCloseTradeDialog: StateFlow<Boolean>
    fun setShowCloseTradeDialog(value: Boolean)
    fun closeTrade()

    fun closeTradeConfirm()

    fun openWalletGuideLink()
    fun openTradeGuideLink()

    fun exportTrade()

    fun openMediation()
}

@Composable
fun TradeFlowScreen() {
    val strings = LocalStrings.current.bisqEasy
    val presenter: ITradeFlowPresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()
    val showCloseTradeDialog = presenter.showCloseTradeDialog.collectAsState().value

    BisqStaticScaffold(
        topBar = { TopBar("Trade - 07b9bab1") }
    ) {
        Box(modifier = Modifier.fillMaxSize().blur(if (showCloseTradeDialog) 12.dp else 0.dp)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {

                TradeHeader(offer)

                BisqGap.V2()

                TradeFlowStepper()

            }
            if (showCloseTradeDialog) {
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

            }
        }
    }
}

@Composable
fun TradeFlowStepper() {
    val presenter: ITradeFlowPresenter = koinInject()
    var expandedStep by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        presenter.steps.forEachIndexed { index, step ->
            StepperSection(
                stepNumber = index + 1,
                isActive = expandedStep == index,
                isLastIndex = index == step.titleKey.lastIndex,
            ) { modifier ->
                Column(modifier = modifier) {
                    BisqText.baseRegular(
                        text = step.getTranslatedTitle().uppercase(),
                        color = if (expandedStep == index) BisqTheme.colors.light1 else BisqTheme.colors.grey2,
                    )

                    AnimatedVisibility(
                        visible = expandedStep == index,
                    ) {
                        when (step.titleKey) {
                            TradeFlowScreenSteps.ACCOUNT_DETAILS.titleKey -> {
                                TradeFlowAccountDetails(onNext = { expandedStep += 1 })
                            }

                            TradeFlowScreenSteps.FIAT_PAYMENT.titleKey -> {
                                TradeFlowFiatPayment(onNext = { expandedStep += 1 })
                            }

                            TradeFlowScreenSteps.BITCOIN_TRANSFER.titleKey -> {
                                TradeFlowBtcPayment(onNext = { expandedStep += 1 })
                            }

                            TradeFlowScreenSteps.TRADE_COMPLETED.titleKey -> {
                                TradeFlowCompleted(onClose = {
                                    presenter.closeTrade()
                                }, onExport = {

                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
