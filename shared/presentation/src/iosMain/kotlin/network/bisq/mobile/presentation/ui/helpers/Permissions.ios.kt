package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class IosNotificationPermissionRequestLauncher(private val onResult: (Boolean) -> Unit) :
    PermissionRequestLauncher {
    override fun launch() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.getNotificationSettingsWithCompletionHandler { settings ->
            when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized -> {
                    dispatch_async(dispatch_get_main_queue()) {
                        onResult(true)
                    }
                }

                UNAuthorizationStatusDenied -> {
                    dispatch_async(dispatch_get_main_queue()) {
                        onResult(false)
                    }
                }

                UNAuthorizationStatusNotDetermined -> {
                    center.requestAuthorizationWithOptions(
                        UNAuthorizationOptionAlert or
                                UNAuthorizationOptionSound or
                                UNAuthorizationOptionBadge
                    ) { granted, error ->
                        dispatch_async(dispatch_get_main_queue()) {
                            onResult(granted)
                        }
                    }
                }

                else -> {
                    dispatch_async(dispatch_get_main_queue()) {
                        onResult(false)
                    }
                }
            }
        }
    }
}

class IosCameraPermissionRequestLauncher(private val onResult: (Boolean) -> Unit) :
    PermissionRequestLauncher {
    override fun launch() {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        when (status) {
            AVAuthorizationStatusAuthorized -> onResult(true)
            AVAuthorizationStatusDenied -> onResult(false)
            AVAuthorizationStatusRestricted -> onResult(false)
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        onResult(granted)
                    }
                }
            }

            else -> onResult(false)
        }
    }
}

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    val launcher = remember(onResult) { IosNotificationPermissionRequestLauncher(onResult) }
    return launcher
}

@Composable
actual fun rememberCameraPermissionLauncher(onResult: (Boolean) -> Unit): PermissionRequestLauncher {
    val launcher = remember(onResult) { IosCameraPermissionRequestLauncher(onResult) }
    return launcher
}
