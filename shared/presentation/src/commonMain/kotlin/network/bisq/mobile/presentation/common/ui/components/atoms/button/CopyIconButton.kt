package network.bisq.mobile.presentation.common.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CheckIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

private const val COPIED_FEEDBACK_MILLIS = 2000L

/**
 * @param showToast set to false where a snackbar cannot be seen (e.g. inside a dialog); the icon
 *   still confirms the copy by briefly turning into a check mark.
 * @param iconSize overrides the icon's intrinsic size when it has to line up with other icons.
 */
@Composable
fun CopyIconButton(
    value: String,
    showToast: Boolean = true,
    iconSize: Dp = Dp.Unspecified,
) {
    val inPreview = LocalInspectionMode.current
    val isTest = LocalIsTest.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MILLIS.milliseconds)
            copied = false
        }
    }

    val presenter: MainPresenter? = if (showToast && !inPreview && !isTest) koinInject() else null
    CopyIconButtonContent(
        copied = copied,
        iconSize = iconSize,
        onClick = {
            scope.launch {
                // Clipboard access can fail (platform restrictions, dead window); a copy
                // button must not escalate that into a crash.
                val result = runCatching { clipboard.setClipEntry(AnnotatedString(value).toClipEntry()) }
                if (result.isFailure) return@launch

                copied = true
                if (showToast && presenter != null) {
                    presenter.showSnackbar("mobile.components.copyIconButton.copied".i18n())
                }
            }
        },
    )
}

@Composable
private fun CopyIconButtonContent(
    copied: Boolean,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    val iconModifier = if (iconSize.isSpecified) Modifier.size(iconSize) else Modifier
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        IconButton(
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
            onClick = onClick,
        ) {
            if (copied) {
                CheckIcon(modifier = iconModifier)
            } else {
                CopyIcon(modifier = iconModifier)
            }
        }
    }
}

@Preview
@ExcludeFromCoverage
@Composable
private fun CopyIconButtonPreview() {
    // Preview wrapped in BisqTheme for proper styling
    BisqTheme.Preview {
        // Create a simple parent to render the button
        Surface(
            color = BisqTheme.colors.backgroundColor,
        ) {
            CopyIconButton(
                value = "Preview copy text",
                showToast = false, // No toast in preview
            )
        }
    }
}

@Preview
@ExcludeFromCoverage
@Composable
private fun CopyIconButtonCopiedPreview() {
    BisqTheme.Preview {
        Surface(color = BisqTheme.colors.backgroundColor) {
            CopyIconButtonContent(copied = true, iconSize = Dp.Unspecified, onClick = {})
        }
    }
}

@Preview
@ExcludeFromCoverage
@Composable
private fun CopyIconButtonSizedIconPreview() {
    BisqTheme.Preview {
        Surface(color = BisqTheme.colors.backgroundColor) {
            CopyIconButtonContent(copied = false, iconSize = 18.dp, onClick = {})
        }
    }
}
