package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable


interface PermissionRequestLauncher {
    fun launch()
}

@Composable
expect fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher

@Composable
expect fun rememberCameraPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher