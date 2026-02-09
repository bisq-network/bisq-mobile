package network.bisq.mobile.presentation.common.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.main.MainPresenter
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun CopyIconButton(
    value: String,
    showToast: Boolean = true,
) {
    val inPreview = LocalInspectionMode.current
    val isTest = LocalIsTest.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val presenter: MainPresenter? = if (!inPreview && !isTest) koinInject() else null
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        IconButton(
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(AnnotatedString(value).toClipEntry())
                }
                if (showToast && presenter != null) {
                    presenter.showSnackbar("mobile.components.copyIconButton.copied".i18n())
                }
            },
        ) {
            CopyIcon()
        }
    }
}

@Preview
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
