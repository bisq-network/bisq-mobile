package network.bisq.mobile.presentation.ui.components.organisms.chat

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme


@Composable
fun UndoIgnoreDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        headline = "error.warning".i18n(),
        headlineColor = BisqTheme.colors.warning,
        headlineLeftIcon = { WarningIcon() },
        message = "mobile.chat.undoIgnoreUserWarn".i18n(),
        confirmButtonText = "user.profileCard.userActions.undoIgnore".i18n(),
        dismissButtonText = "action.cancel".i18n(),
        verticalButtonPlacement = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}