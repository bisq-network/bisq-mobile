package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun WebLinkConfirmationDialog(
    link: String,
    headline: String = "hyperlinks.openInBrowser.attention.headline".i18n(),
    headlineColor: Color = BisqTheme.colors.primary,
    headlineLeftIcon: (@Composable () -> Unit)? = { InfoGreenIcon() },
    message: String = "hyperlinks.openInBrowser.attention".i18n(link),
    confirmButtonText: String = "confirmation.yes".i18n(),
    dismissButtonText: String = "hyperlinks.openInBrowser.no".i18n(),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    ConfirmationDialog(
        headline = headline,
        headlineColor = headlineColor,
        headlineLeftIcon = headlineLeftIcon,
        message = message,
        confirmButtonText = confirmButtonText,
        dismissButtonText = dismissButtonText,
        onConfirm = {
            uriHandler.openUri(link)
            onConfirm.invoke()
        },
        onDismiss = { toCopy ->
            if (toCopy) {
                scope.launch {
                    clipboard.setClipEntry(AnnotatedString(link).toClipEntry())
                }
            }
            onDismiss()
        },
        closeButton = true,
        horizontalAlignment = Alignment.Start,
        verticalButtonPlacement = true
    )
}