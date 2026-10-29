package network.bisq.mobile.presentation.common.ui.components.organisms.trades

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Bitcoin payment data that requests another amount than the trade amount. The seller's node rejects it, so unlike
 * the invalid address dialog there is no "proceed anyway": the buyer has to enter other data.
 */
@Composable
fun OtherAmountWarningDialog(
    tradeAmountWithCode: String,
    onDismiss: () -> Unit,
) {
    WarningConfirmationDialog(
        message = "mobile.tradeState.buyer.phase1a.otherAmount.warning".i18n(tradeAmountWithCode),
        confirmButtonText = "action.close".i18n(),
        dismissButtonText = EMPTY_STRING,
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.Start,
    )
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OtherAmountWarningDialogPreview() {
    BisqTheme.Preview {
        OtherAmountWarningDialog(tradeAmountWithCode = "0.0025 BTC", onDismiss = {})
    }
}
