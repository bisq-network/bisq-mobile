package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants


@Composable
fun OpenMediationDialog(
    onCancelConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {

    WarningConfirmationDialog(
        headline = "bisqEasy.mediation.request.confirm.headline".i18n(),
        message = "bisqEasy.mediation.request.confirm.msg".i18n(),
        confirmButtonText = "bisqEasy.mediation.request.confirm.openMediation".i18n(),
        horizontalAlignment = Alignment.Start,
        marginTop = BisqUIConstants.ScreenPaddingHalf,
        onDismiss = onDismiss,
        onConfirm = onCancelConfirm
    )

}