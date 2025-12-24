package network.bisq.mobile.presentation.common.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.compose.koinInject

@Composable
fun CopyIconButton(
    value: String,
    showToast: Boolean = true,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val presenter: MainPresenter = koinInject()
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        IconButton(
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(AnnotatedString(value).toClipEntry())
                }
                if (showToast) {
                    presenter.showSnackbar("mobile.components.copyIconButton.copied".i18n())
                }
            },
        ) {
            CopyIcon()
        }
    }
}
