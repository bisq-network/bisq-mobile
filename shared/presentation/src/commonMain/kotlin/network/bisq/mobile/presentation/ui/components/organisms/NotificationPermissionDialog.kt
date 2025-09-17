package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog

@Composable
fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        headline = "",
        message = "mobile.permissions.notifications.explanation".i18n(),
        confirmButtonText = "action.grantPermission".i18n(),
        dismissButtonText = "action.dontAskAgain".i18n(),
        verticalButtonPlacement = true,
        onConfirm = onConfirm,
        onDismiss = { _ -> onDismiss() },
    )
}