package network.bisq.mobile.presentation.ui.components.molecules.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun WebLinkConfirmationDialog(
    link: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        message = "hyperlinks.openInBrowser.attention.headline".i18n(),
        messageColor = BisqTheme.colors.primary,
        messageLeftIcon = { InfoGreenIcon() },
        subMessage = "hyperlinks.openInBrowser.attention".i18n(link),
        cancelButtonText = "hyperlinks.openInBrowser.no".i18n(),
        confirmButtonText = "confirmation.yes".i18n(),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        horizontalAlignment =  Alignment.Start,
        verticalButtonPlacement = true
    )
}