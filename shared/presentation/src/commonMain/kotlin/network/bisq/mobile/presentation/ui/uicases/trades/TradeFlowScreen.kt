package network.bisq.mobile.presentation.ui.uicases.trades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.organisms.trades.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

enum class TradeFlowScreenSteps(val title: String) {
    ACCOUNT_DETAILS(title = "ACCOUNT DETAILS"),
    FIAT_PAYMENT(title = "FIAT PAYMENT"),
    BITCOIN_TRANSFER(title = "BITCOIN TRANSFER"),
    TRADE_COMPLETED(title = "TRADE COMPLETED")
}

interface ITradeFlowPresenter : ViewPresenter {
    // TODO: Update later to refer to a single state specific object
    val offerListItems: StateFlow<List<OfferListItem>>

    val steps: List<String>
}

@Composable
fun TradeFlowScreen() {
    val strings = LocalStrings.current.bisqEasy
    val presenter: ITradeFlowPresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()

    BisqStaticScaffold(
        topBar = { TopBar("Trade - 07b9bab1") }
    ) {

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {

            TradeHeader(offer)

            Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))

            TradeFlowStepper()

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
                isLastIndex = index == step.lastIndex,
            ) { modifier ->
                Column(modifier = modifier) {
                    BisqText.baseRegular(
                        text = step,
                        color = if (expandedStep == index) BisqTheme.colors.light1 else BisqTheme.colors.grey2,
                    )

                    AnimatedVisibility(
                        visible = expandedStep == index,
                    ) {
                        when (step) {
                            TradeFlowScreenSteps.ACCOUNT_DETAILS.title -> {
                                TradeFlow01AccountDetails(onNext = { expandedStep += 1 })
                            }

                            TradeFlowScreenSteps.FIAT_PAYMENT.title -> {
                                TradeFlow02FiatPayment(onNext = { expandedStep += 1 })
                            }

                            TradeFlowScreenSteps.BITCOIN_TRANSFER.title -> {
                                TradeFlow03BtcPayment(onNext = { expandedStep += 1 })
                            }

                            TradeFlowScreenSteps.TRADE_COMPLETED.title -> {
                                TradeFlow04Completed(onNext = { expandedStep += 1 })
                            }
                        }
                    }
                }
            }
        }
    }
}
