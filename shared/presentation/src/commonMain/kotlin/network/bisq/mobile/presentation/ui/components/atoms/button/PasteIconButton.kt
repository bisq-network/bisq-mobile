package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import network.bisq.mobile.presentation.ui.components.atoms.icons.PasteIcon

@Composable
fun PasteIconButton(
    onPaste: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    IconButton(
        onClick = {
            val value = clipboardManager.getText()
            if (value != null) {
                onPaste(value.text)
            }
        }
    ) {
        PasteIcon()
    }
}