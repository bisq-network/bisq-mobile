package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.ui.components.atoms.button.FloatingButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.ChatIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.PaymentProofType
import network.bisq.mobile.presentation.ui.components.organisms.trades.*
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.*
import org.koin.compose.koinInject

@Composable
fun OpenTradeScreen() {
    val presenter: OpenTradePresenter = koinInject()
    val headerPresenter: TradeDetailsHeaderPresenter = koinInject()
    val buyerState1aPresenter: BuyerState1aPresenter = koinInject()
    val sellerState3aPresenter: SellerState3aPresenter = koinInject()
    val buyerState4Presenter: BuyerState4Presenter = koinInject()
    val sellerState4Presenter: SellerState4Presenter = koinInject()

    RememberPresenterLifecycle(presenter)
    val focusManager = LocalFocusManager.current

    val tradeAbortedBoxVisible by presenter.tradeAbortedBoxVisible.collectAsState()
    val tradeProcessBoxVisible by presenter.tradeProcessBoxVisible.collectAsState()
    val isInMediation by presenter.isInMediation.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val tradeCloseType by headerPresenter.tradeCloseType.collectAsState()
    val showInterruptionConfirmationDialog by headerPresenter.showInterruptionConfirmationDialog.collectAsState()
    val showMediationConfirmationDialog by headerPresenter.showMediationConfirmationDialog.collectAsState()
    val newMsgCount by presenter.newMsgCount.collectAsState()

    val buyerState1aAddressFieldType by buyerState1aPresenter.bitcoinLnAddressFieldType.collectAsState()
    val buyerState1aShowInvalidAddressDialog by buyerState1aPresenter.showInvalidAddressDialog.collectAsState()
    val sellerState3aShowInvalidAddressDialog by sellerState3aPresenter.showInvalidAddressDialog.collectAsState()
    val sellerState3aIsLightning by sellerState3aPresenter.isLightning.collectAsState()
    val buyerState4ShowCloseTradeDialog by buyerState4Presenter.showCloseTradeDialog.collectAsState()
    val sellerState4ShowCloseTradeDialog by sellerState4Presenter.showCloseTradeDialog.collectAsState()

    val shouldBlurBg by remember {
        derivedStateOf {
            showInterruptionConfirmationDialog ||
                    showMediationConfirmationDialog ||
                    buyerState1aShowInvalidAddressDialog ||
                    sellerState3aShowInvalidAddressDialog ||
                    buyerState4ShowCloseTradeDialog ||
                    sellerState4ShowCloseTradeDialog
        }
    }

    RememberPresenterLifecycle(presenter, {
        presenter.setTradePaneScrollState(scrollState)
        presenter.setUIScope(scope)
    })

    BisqStaticScaffold(
        topBar = { TopBar("Trade ID: ${presenter.selectedTrade.value?.shortTradeId}") },
        floatingButton = {
            val icon = @Composable {
                FloatingButton(
                    onClick = { presenter.onOpenChat() },
                ) {
                    ChatIcon(modifier = Modifier.size(34.dp))
                }
            }

            if (newMsgCount == 0) {
                icon()
            } else {
                BadgedBox(badge = {
                    AnimatedBadge(showAnimation = true, xOffset = (-4).dp) {
                        BisqText.xsmallLight(
                            newMsgCount.toString(),
                            textAlign = TextAlign.Center, color = BisqTheme.colors.dark_grey20
                        )
                    }
                }) {
                    icon()
                }
            }
        },
        shouldBlurBg = shouldBlurBg,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
            ) {
                if (presenter.selectedTrade.value != null) {
                    TradeDetailsHeader()

                    if (isInMediation) {
                        BisqGap.V2()
                        MediationBanner()
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
        }
    }

    if (showInterruptionConfirmationDialog) {
        CancelTradeDialog(
            onCancelConfirm = { headerPresenter.onInterruptTrade() },
            onDismiss = { headerPresenter.onCloseInterruptionConfirmationDialog() },
            isBuyer = headerPresenter.directionEnum.isBuy,
            isRejection = tradeCloseType == TradeDetailsHeaderPresenter.TradeCloseType.REJECT
        )
    }

    if (showMediationConfirmationDialog) {
        OpenMediationDialog(
            onCancelConfirm = headerPresenter::onOpenMediation,
            onDismiss = headerPresenter::onCloseMediationConfirmationDialog,
        )
    }

    if (buyerState1aShowInvalidAddressDialog) {
        InvalidAddressConfirmationDialog(
            addressType = buyerState1aAddressFieldType,
            onConfirm = buyerState1aPresenter::onSend,
            onDismiss = { buyerState1aPresenter.setShowInvalidAddressDialog(false) },
        )
    }

    if (sellerState3aShowInvalidAddressDialog) {
        InvalidPaymentProofConfirmationDialog(
            paymentProofType = if (sellerState3aIsLightning) PaymentProofType.LightningPreImage else PaymentProofType.BitcoinTx,
            onDismiss = { sellerState3aPresenter.setShowInvalidAddressDialog(false) },
            onConfirm = sellerState3aPresenter::confirmSend,
        )
    }

    if (buyerState4ShowCloseTradeDialog) {
        CloseTradeDialog(
            onDismiss = buyerState4Presenter::onDismissCloseTrade,
            onConfirm = buyerState4Presenter::onConfirmCloseTrade,
        )
    }

    if (sellerState4ShowCloseTradeDialog) {
        CloseTradeDialog(
            onDismiss = sellerState4Presenter::onDismissCloseTrade,
            onConfirm = sellerState4Presenter::onConfirmCloseTrade,
        )
    }

}

