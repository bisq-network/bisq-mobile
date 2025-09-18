package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.ui.helpers.toClipEntry
import org.koin.compose.koinInject

@Composable
fun CopyIconButton(value: String, showToast: Boolean = true) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val presenter: MainPresenter = koinInject()
    IconButton(
        onClick = {
            scope.launch {
                clipboard.setClipEntry(AnnotatedString(value).toClipEntry())
            }
            if (showToast) {
                presenter.showSnackbar("mobile.components.copyIconButton.copied".i18n())
            }
        }
    ) {
        CopyIcon()
    }
}