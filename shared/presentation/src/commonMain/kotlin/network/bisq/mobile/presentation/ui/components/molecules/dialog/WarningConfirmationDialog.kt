package network.bisq.mobile.presentation.ui.components.molecules.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun WarningConfirmationDialog(
    subMessage: String = "",
    confirmButtonText: String = "Yes",
    cancelButtonText: String = "No",
    marginTop: Dp = BisqUIConstants.ScreenPadding5X,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalButtonPlacement: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        message = "popup.headline.warning".i18n(),
        messageColor = BisqTheme.colors.warning,
        messageLeftIcon = { WarningIcon() },
        subMessage = subMessage,
        cancelButtonText = cancelButtonText,
        confirmButtonText = confirmButtonText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        marginTop = marginTop,
        horizontalAlignment = horizontalAlignment,
        verticalButtonPlacement = verticalButtonPlacement
    )
}