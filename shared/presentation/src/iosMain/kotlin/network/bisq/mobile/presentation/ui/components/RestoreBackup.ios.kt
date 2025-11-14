package network.bisq.mobile.presentation.ui.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CompletableDeferred

@Composable
actual fun RestoreBackup(onRestoreBackup: (String, String?, ByteArray) -> CompletableDeferred<String?>) {
    // Not used in client mode
}