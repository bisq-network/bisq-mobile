package network.bisq.mobile.domain.service.push_notification

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware

/**
 * Service facade for managing push notification registration.
 * Handles device token registration with the trusted node.
 */
interface PushNotificationServiceFacade : LifeCycleAware {
    /**
     * Whether push notifications are enabled by the user.
     */
    val isPushNotificationsEnabled: StateFlow<Boolean>

    /**
     * Whether the device is currently registered for push notifications.
     */
    val isDeviceRegistered: StateFlow<Boolean>

    /**
     * The current device token, if available.
     */
    val deviceToken: StateFlow<String?>

    /**
     * Request permission for push notifications from the OS.
     * On iOS, this will prompt the user for permission.
     * @return true if permission was granted, false otherwise
     */
    suspend fun requestPermission(): Boolean

    /**
     * Register the device for push notifications.
     * This will request a device token from APNs/FCM and send it to the trusted node.
     * @return Result indicating success or failure
     */
    suspend fun registerForPushNotifications(): Result<Unit>

    /**
     * Unregister the device from push notifications.
     * @return Result indicating success or failure
     */
    suspend fun unregisterFromPushNotifications(): Result<Unit>

    /**
     * Called when a new device token is received from APNs/FCM.
     * This is typically called from platform-specific code (AppDelegate on iOS).
     */
    suspend fun onDeviceTokenReceived(token: String)

    /**
     * Called when device token registration fails.
     */
    suspend fun onDeviceTokenRegistrationFailed(error: Throwable)
}
