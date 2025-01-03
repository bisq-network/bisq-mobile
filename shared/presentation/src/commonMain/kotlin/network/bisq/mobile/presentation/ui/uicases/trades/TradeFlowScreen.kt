package network.bisq.mobile.presentation.ui.uicases.trades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.organisms.trades.CloseTradeCard
import network.bisq.mobile.presentation.ui.components.organisms.trades.StepperSection
import network.bisq.mobile.presentation.ui.components.organisms.trades.TradeFlowAccountDetails
import network.bisq.mobile.presentation.ui.components.organisms.trades.TradeFlowBtcPayment
import network.bisq.mobile.presentation.ui.components.organisms.trades.TradeFlowCompleted
import network.bisq.mobile.presentation.ui.components.organisms.trades.TradeFlowFiatPayment
import network.bisq.mobile.presentation.ui.components.organisms.trades.TradeHeader
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface ITradeFlowPresenter : ViewPresenter {
    // TODO: Update later to refer to a single state specific object
    val offerListItems: StateFlow<List<OfferListItemVO>>

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
