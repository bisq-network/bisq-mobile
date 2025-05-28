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
    headline: String = "popup.headline.warning".i18n(),
    message: String = "",
    confirmButtonText: String = "confirmation.ok".i18n(),
    dismissButtonText: String = "action.cancel".i18n(),
    marginTop: Dp = BisqUIConstants.ScreenPadding8X,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalButtonPlacement: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        headline = headline,
        headlineColor = BisqTheme.colors.warning,
        headlineLeftIcon = { WarningIcon() },
        message = message,
        confirmButtonText = confirmButtonText,
        dismissButtonText = dismissButtonText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        marginTop = marginTop,
        horizontalAlignment = horizontalAlignment,
        verticalButtonPlacement = verticalButtonPlacement
    )
}