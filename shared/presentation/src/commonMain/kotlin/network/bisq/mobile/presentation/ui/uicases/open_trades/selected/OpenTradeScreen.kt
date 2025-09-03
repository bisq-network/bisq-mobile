package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.PaymentProofType
import network.bisq.mobile.presentation.ui.components.organisms.chat.UndoIgnoreDialog
import network.bisq.mobile.presentation.ui.components.organisms.trades.CancelTradeDialog
import network.bisq.mobile.presentation.ui.components.organisms.trades.CloseTradeDialog
import network.bisq.mobile.presentation.ui.components.organisms.trades.InvalidAddressConfirmationDialog
import network.bisq.mobile.presentation.ui.components.organisms.trades.InvalidPaymentProofConfirmationDialog
import network.bisq.mobile.presentation.ui.components.organisms.trades.OpenMediationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.spaceBetweenWithMin
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState1aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState4Presenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState3aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState4Presenter
import org.koin.compose.koinInject

@Composable
fun OpenTradeScreen() {
    val presenter: OpenTradePresenter = koinInject()
    val headerPresenter: TradeDetailsHeaderPresenter = koinInject()
    val buyerState1aPresenter: BuyerState1aPresenter = koinInject()
    val sellerState3aPresenter: SellerState3aPresenter = koinInject()
    val buyerState4Presenter: BuyerState4Presenter = koinInject()
    val sellerState4Presenter: SellerState4Presenter = koinInject()

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    RememberPresenterLifecycle(presenter, {
        presenter.setTradePaneScrollState(scrollState)
        presenter.setUIScope(scope)
    })

    val focusManager = LocalFocusManager.current

    val selectedTrade by presenter.selectedTrade.collectAsState()
    val tradeAbortedBoxVisible by presenter.tradeAbortedBoxVisible.collectAsState()
    val tradeProcessBoxVisible by presenter.tradeProcessBoxVisible.collectAsState()
    val isInMediation by presenter.isInMediation.collectAsState()
    val tradeCloseType by headerPresenter.tradeCloseType.collectAsState()
    val showInterruptionConfirmationDialog by headerPresenter.showInterruptionConfirmationDialog.collectAsState()
    val showMediationConfirmationDialog by headerPresenter.showMediationConfirmationDialog.collectAsState()
    val mediationError by headerPresenter.mediationError.collectAsState()
    val showUndoIgnoreDialog by presenter.showUndoIgnoreDialog.collectAsState()
    val newMsgCount by presenter.newMsgCount.collectAsState()
    val lastChatMsg by presenter.lastChatMsg.collectAsState()
    val isInteractive by presenter.isInteractive.collectAsState()
    val isIgnoredUser by presenter.isUserIgnored.collectAsState()

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
                    mediationError.isNotBlank() ||
                    buyerState1aShowInvalidAddressDialog ||
                    sellerState3aShowInvalidAddressDialog ||
                    buyerState4ShowCloseTradeDialog ||
                    sellerState4ShowCloseTradeDialog ||
                    showUndoIgnoreDialog
        }
    }

    BisqStaticScaffold(
        topBar = { TopBar("mobile.bisqEasy.openTrades.title".i18n(selectedTrade?.shortTradeId ?: "")) },
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
                if (selectedTrade != null) {
                    TradeDetailsHeader()

                    if (isIgnoredUser) {
                        BisqGap.V2()
                        Row(
                            horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BisqText.baseRegular(
                                modifier = Modifier.weight(1f),
                                text = "mobile.bisqEasy.openTrades.warnIgnoredUser".i18n(),
                                color = BisqTheme.colors.warning
                            )
                            BisqButton(
                                text = "user.profileCard.userActions.undoIgnore".i18n(),
                                type = BisqButtonType.GreyOutline,
                                padding = PaddingValues(
                                    horizontal = BisqUIConstants.ScreenPaddingHalf,
                                    vertical = BisqUIConstants.ScreenPaddingHalf,
                                ),
                                onClick = presenter::onOpenUndoIgnoreDialog
                            )
                        }
                    }

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

                    BisqGap.V2()

                    TradeChatRow(
                        presenter = presenter,
                        lastChatMsg = lastChatMsg,
                        newMsgCount = newMsgCount,
                        enabled = isInteractive,
                    )
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

    if (mediationError.isNotBlank()) {
        WarningConfirmationDialog(
            message = mediationError,
            dismissButtonText = "",
            onConfirm = headerPresenter::onCloseMediationErrorDialog,
            onDismiss = { headerPresenter.onCloseMediationErrorDialog() },
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

    if (showUndoIgnoreDialog) {
        UndoIgnoreDialog(
            onConfirm = presenter::onConfirmedUndoIgnoreUser,
            onDismiss = presenter::hideUndoIgnoreDialog,
        )
    }

}

