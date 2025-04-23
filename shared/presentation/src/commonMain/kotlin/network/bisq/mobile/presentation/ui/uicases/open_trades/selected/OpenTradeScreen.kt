package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.presentation.ui.components.atoms.button.FloatingButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.ChatIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.organisms.trades.CancelTradeDialog
import network.bisq.mobile.presentation.ui.components.organisms.trades.OpenMediationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun OpenTradeScreen() {
    val presenter: OpenTradePresenter = koinInject()
    val headerPresenter: TradeDetailsHeaderPresenter = koinInject()

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

    RememberPresenterLifecycle(presenter, {
        presenter.setTradePaneScrollState(scrollState)
        presenter.setUIScope(scope)
    })

    BisqStaticScaffold(
        topBar = { TopBar("Trade ID: ${presenter.selectedTrade.value?.shortTradeId}") },
        floatingButton = {
            FloatingButton(
                onClick = { presenter.onOpenChat() },
            ) {
                ChatIcon(modifier = Modifier.size(34.dp))
            }
        },
        shouldBlurBg = showInterruptionConfirmationDialog || showMediationConfirmationDialog,
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

}

