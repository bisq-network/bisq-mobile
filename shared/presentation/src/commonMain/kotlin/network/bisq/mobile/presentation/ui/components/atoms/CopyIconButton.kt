package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import network.bisq.mobile.presentation.ui.components.atoms.icons.CopyIcon

@Composable
fun CopyIconButton(value: String) {
    val clipboardManager = LocalClipboardManager.current
    IconButton(
        onClick = {
            clipboardManager.setText(buildAnnotatedString { append(value) })
        }
    ) {
        CopyIcon()
    }
}