package network.bisq.mobile.presentation.ui.helpers

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


class AndroidPermissionLauncher<O>(
    private val permission: String,
    private val managedActivityResultLauncher: ManagedActivityResultLauncher<String, O>,
) : PermissionRequestLauncher {

    override fun launch() {
        managedActivityResultLauncher.launch(permission)
    }
}

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult,
    )
    val androidLauncher = remember(launcher) {
        AndroidPermissionLauncher("android.permission.POST_NOTIFICATIONS", launcher)
    }
    return androidLauncher
}