package network.bisq.mobile.presentation.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun RestoreBackup(onRestoreBackup: (String, String?, ByteArray) -> Unit) {
    // Not used in client mode
}