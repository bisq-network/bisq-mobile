package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants


@Composable
fun CancelTradeDialog(
    onCancelConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isBuyer: Boolean = true,
    isRejection: Boolean,
) {

    val part2: String = "bisqEasy.openTrades.cancelTrade.warning.part2".i18n()
    val warningText1 = if (isRejection) {
        "bisqEasy.openTrades.rejectTrade.warning".i18n()
    } else {
        if (isBuyer)
            "bisqEasy.openTrades.cancelTrade.warning.buyer".i18n(part2)
        else
            "bisqEasy.openTrades.cancelTrade.warning.seller".i18n(part2)
    }

    WarningConfirmationDialog(
        subMessage = warningText1,
        horizontalAlignment = Alignment.Start,
        marginTop = if (isRejection)
            BisqUIConstants.ScreenPadding5X
        else
            BisqUIConstants.ScreenPaddingHalf,
        onDismiss = onDismiss,
        onConfirm = onCancelConfirm
    )

}