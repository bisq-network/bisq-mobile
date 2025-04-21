package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog

@Composable
fun CloseTradeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ConfirmationDialog(
        headline = "",
        message = "bisqEasy.openTrades.closeTrade.warning.completed".i18n(),
        dismissButtonText = "action.cancel".i18n(),
        confirmButtonText = "bisqEasy.openTrades.confirmCloseTrade".i18n(),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        verticalButtonPlacement = true
    )
}