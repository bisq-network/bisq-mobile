package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BatteryOptimizationsDialog(
    onConfirm: () -> Unit,
    onDismiss: (dontAskAgain: Boolean) -> Unit,
) {
    ConfirmationDialog(
        headline = "",
        message = "mobile.platform.settings.batteryOptimizations.explanation".i18n(),
        confirmButtonText = "mobile.action.openSettings".i18n(),
        dismissButtonText = "mobile.action.dontAskAgain".i18n(),
        verticalButtonPlacement = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
fun BatteryOptimizationsDialogPreview() {
    BisqTheme.Preview {
        BatteryOptimizationsDialog(
            onConfirm = {},
            onDismiss = { _ -> },
        )
    }
}
