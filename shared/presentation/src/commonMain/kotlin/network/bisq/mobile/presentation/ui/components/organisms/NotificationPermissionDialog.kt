package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog

@Composable
fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: (dontAskAgain: Boolean) -> Unit,
) {
    val platform = remember { getPlatformInfo().type }
    ConfirmationDialog(
        headline = "",
        message = if (platform == PlatformType.ANDROID)
            "mobile.permissions.notifications.explanation.android".i18n()
        else "mobile.permissions.notifications.explanation".i18n(),
        confirmButtonText = "action.grantPermission".i18n(),
        dismissButtonText = "action.dontAskAgain".i18n(),
        verticalButtonPlacement = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}