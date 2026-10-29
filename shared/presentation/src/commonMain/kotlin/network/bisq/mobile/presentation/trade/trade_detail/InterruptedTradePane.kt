package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.settings.support.SupportChannelLink
import org.koin.compose.koinInject

@Composable
fun InterruptedTradePane(
    showSupportChannel: Boolean,
    onOpenSupportChannel: () -> Unit,
) {
    val presenter: InterruptedTradePresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()
    InterruptedTradeContent(
        uiState = uiState,
        onAction = presenter::onAction,
        showSupportChannel = showSupportChannel,
        onOpenSupportChannel = onOpenSupportChannel,
    )
}

@Composable
private fun InterruptedTradeContent(
    uiState: InterruptedTradeUiState,
    onAction: (InterruptedTradeUiAction) -> Unit,
    showSupportChannel: Boolean,
    onOpenSupportChannel: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = BisqTheme.colors.dark_grey40),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            uiState.interruptedTradeInfo?.let { info ->
                // TODO: Add orange warn icon
                BisqText.BaseMedium(
                    text = info,
                    color = BisqTheme.colors.warning,
                )
            }
            uiState.errorMessage?.let { error ->
                // TODO: Add red warn icon
                BisqText.BaseMedium(
                    text = error,
                    color = BisqTheme.colors.danger,
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.End,
        ) {
            if (uiState.errorMessage != null && showSupportChannel && !uiState.isInMediation) {
                SupportChannelLink(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = onOpenSupportChannel,
                )
                BisqGap.H1()
            }
            if (!uiState.isInMediation && uiState.isReportToMediatorButtonVisible) {
                BisqButton(
                    modifier = Modifier.fillMaxHeight(),
                    text = "bisqEasy.openTrades.reportToMediator".i18n(),
                    onClick = { onAction(InterruptedTradeUiAction.OnReportToMediator) },
                    type = BisqButtonType.Outline,
                    color = BisqTheme.colors.primary,
                    borderColor = BisqTheme.colors.primary,
                    disabled = !uiState.isReportToMediatorEnabled,
                )
                BisqGap.H1()
            }
            BisqButton(
                modifier = Modifier.fillMaxHeight(),
                text = "action.close".i18n(),
                onClick = { onAction(InterruptedTradeUiAction.OnCloseTrade) },
                disabled = !uiState.isCloseTradeEnabled,
            )
        }

        if (uiState.isNoMediatorAvailableWarningVisible) {
            WarningConfirmationDialog(
                onConfirm = { onAction(InterruptedTradeUiAction.OnDismissNoMediatorAvailableWarning) },
                onDismiss = { },
                dismissButtonText = EMPTY_STRING,
                confirmButtonText = "action.close".i18n(),
                message = "bisqEasy.takeOffer.noMediatorAvailable.warning".i18n(),
            )
        }

        if (uiState.isMediationRequestedDialogVisible) {
            ConfirmationDialog(
                onConfirm = { onAction(InterruptedTradeUiAction.OnDismissMediationRequestedDialog) },
                headline = "bisqEasy.mediation.request.feedback.headline".i18n(),
                message = "bisqEasy.mediation.request.feedback.msg".i18n(),
                confirmButtonText = "action.close".i18n(),
                dismissButtonText = EMPTY_STRING,
                onDismiss = { onAction(InterruptedTradeUiAction.OnDismissMediationRequestedDialog) },
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun InterruptedTradeContent_CancelledByPeerPreview() {
    BisqTheme.Preview {
        InterruptedTradeContent(
            uiState =
                InterruptedTradeUiState(
                    interruptedTradeInfo = "Your trade peer has cancelled the trade.",
                    isReportToMediatorButtonVisible = true,
                ),
            onAction = {},
            showSupportChannel = true,
            onOpenSupportChannel = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun InterruptedTradeContent_FailedPreview() {
    BisqTheme.Preview {
        InterruptedTradeContent(
            uiState = InterruptedTradeUiState(errorMessage = "The trade failed: protocol error."),
            onAction = {},
            showSupportChannel = true,
            onOpenSupportChannel = {},
        )
    }
}
