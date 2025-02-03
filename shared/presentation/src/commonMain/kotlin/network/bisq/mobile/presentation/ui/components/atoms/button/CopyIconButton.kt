package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.atoms.icons.CopyIcon
import org.koin.compose.koinInject

@Composable
fun CopyIconButton(value: String, showToast: Boolean = true) {
    val clipboardManager = LocalClipboardManager.current
    val presenter: MainPresenter = koinInject()
    IconButton(
        onClick = {
            clipboardManager.setText(buildAnnotatedString { append(value) })
            if (showToast) {
                presenter.showSnackbar("Text copied")
            }
        }
    ) {
        CopyIcon()
    }
}