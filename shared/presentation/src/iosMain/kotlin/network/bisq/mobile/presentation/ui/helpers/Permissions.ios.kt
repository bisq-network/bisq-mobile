package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationPermissionRequestLauncher(private val onResult: (Boolean) -> Unit) : PermissionRequestLauncher {
    override fun launch() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.getNotificationSettingsWithCompletionHandler { settings ->
            when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized -> onResult(true)
                UNAuthorizationStatusDenied -> onResult(false)
                UNAuthorizationStatusNotDetermined -> {
                    center.requestAuthorizationWithOptions(
                        UNAuthorizationOptionAlert or
                                UNAuthorizationOptionSound or
                                UNAuthorizationOptionBadge
                    ) { granted, error ->
                        onResult(granted)
                    }
                }

                else -> onResult(false)
            }
        }
    }
}

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    val launcher = remember(onResult) { IosNotificationPermissionRequestLauncher(onResult) }
    return launcher
}