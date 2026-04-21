package network.bisq.mobile.presentation.common.ui.components.atoms.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.CancellationException
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun LinkButton(
    text: String,
    link: String,
    modifier: Modifier = Modifier,
    type: BisqButtonType = BisqButtonType.Underline,
    color: Color = BisqTheme.colors.primary,
    padding: PaddingValues = PaddingValues(all = BisqUIConstants.ScreenPaddingHalf),
    onClick: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    fullWidth: Boolean = false,
    openConfirmation: Boolean = true,
    forceConfirm: Boolean = false,
    leftIcon: (@Composable () -> Unit)? = null,
    rightIcon: (@Composable () -> Unit)? = null,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    BisqButton(
        text = text,
        color = color,
        type = type,
        padding = padding,
        fullWidth = fullWidth,
        onClick = {
            if (openConfirmation) {
                showConfirmDialog = true
            } else {
                if (link.isBlank()) {
                    onClick?.invoke()
                    return@BisqButton
                }
                try {
                    uriHandler.openUri(link)
                    onClick?.invoke()
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    onError?.invoke(t)
                }
            }
        },
        modifier = modifier,
        leftIcon = leftIcon,
        rightIcon = rightIcon,
    )

    if (showConfirmDialog) {
        WebLinkConfirmationDialog(
            link = link,
            forceConfirm = forceConfirm,
            onConfirm = {
                onClick?.invoke()
                showConfirmDialog = false
            },
            onDismiss = {
                showConfirmDialog = false
            },
            onError = {
                showConfirmDialog = false
                onError?.invoke(RuntimeException("Web link confirmation failed"))
            },
        )
    }
}
