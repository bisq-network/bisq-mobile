
package network.bisq.mobile.presentation.ui.components.atoms.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.components.atoms.icons.PasteIcon
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.helpers.readText

@Composable
fun PasteIconButton(
    onPaste: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified) {
        IconButton(
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
            onClick = {
                scope.launch {
                    clipboard.getClipEntry()?.readText()?.let { text ->
                        onPaste(text)
                    }
                }
            }
        ) {
            PasteIcon()
        }
    }
}