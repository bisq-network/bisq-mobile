package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.molecules.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

// TODO: Don't show again checkbox
@Composable
fun LinkButton(
    text: String,
    link: String,
    type: BisqButtonType = BisqButtonType.Clear,
    padding: PaddingValues = PaddingValues(all = BisqUIConstants.Zero),
    onClick: (() -> Unit)? = null,
    fullWidth: Boolean = false,
    openConfirmation: Boolean = true,
    modifier: Modifier = Modifier,
) {

    val clipboardManager = LocalClipboardManager.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    BisqButton(
        text,
        color = BisqTheme.colors.primary,
        type = type,
        padding = padding,
        fullWidth = fullWidth,
        onClick = {
            if (openConfirmation) {
                showConfirmDialog = true
            } else {
                onClick?.invoke()
            }

        },
        modifier = modifier
    )


    if (showConfirmDialog) {
        WebLinkConfirmationDialog(
            link = link,
            onConfirm = {
                onClick?.invoke()
                showConfirmDialog = false
            },
            onDismiss = {
                clipboardManager.setText(buildAnnotatedString { append(link) })
                showConfirmDialog = false
            }
        )
    }
}