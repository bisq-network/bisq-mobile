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
     * On Android, this enables Firebase auto-init (off by default for privacy)
     * and registers with FCM.
     * @return Result containing the device token or an error
     */
    suspend fun requestDeviceToken(): Result<String>

    /**
     * Revoke the current device token and stop talking to the upstream push
     * provider. On Android this deletes the FCM token and disables Firebase
     * auto-init so no further connection to Google's servers is opened until
     * the user opts in again. On iOS this is a no-op — APNs is governed
     * entirely by system permission.
     */
    suspend fun revokeDeviceToken(): Result<Unit> = Result.success(Unit)
}
