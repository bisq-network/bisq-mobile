package network.bisq.mobile.client.common.domain.service.push_notification

/**
 * Platform-specific provider for push notification device tokens.
 * Implemented differently on iOS (APNs) and Android (FCM).
 */
interface PushNotificationTokenProvider {
    /**
     * Request permission for push notifications from the OS.
     * @return true if permission was granted, false otherwise
     */
    suspend fun requestPermission(): Boolean

    /**
     * Request a device token from the platform's push notification service.
     * On iOS, this registers with APNs.
     * On Android, this registers with FCM.
     * @return Result containing the device token or an error
     */
    suspend fun requestDeviceToken(): Result<String>
}
