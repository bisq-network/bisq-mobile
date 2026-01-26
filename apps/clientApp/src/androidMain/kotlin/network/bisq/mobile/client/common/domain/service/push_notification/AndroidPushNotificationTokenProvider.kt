package network.bisq.mobile.client.common.domain.service.push_notification

import network.bisq.mobile.domain.utils.Logging

/**
 * Android implementation of PushNotificationTokenProvider.
 * Uses Firebase Cloud Messaging (FCM) to get a device token.
 *
 * TODO: Implement FCM integration when Android push notifications are needed.
 * For now, this is a stub that returns an error.
 */
class AndroidPushNotificationTokenProvider :
    PushNotificationTokenProvider,
    Logging {
    override suspend fun requestPermission(): Boolean {
        // Android 13+ requires POST_NOTIFICATIONS permission
        // For now, return false as FCM integration and permission handling are not implemented yet
        log.w { "Android push notification permission check not implemented - FCM integration required" }
        return false
    }

    override suspend fun requestDeviceToken(): Result<String> {
        // TODO: Implement FCM token retrieval
        // FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        //     if (task.isSuccessful) {
        //         val token = task.result
        //         // Use token
        //     }
        // }
        log.w { "Android push notification token provider not yet implemented" }
        return Result.failure(
            PushNotificationException("Android push notifications not yet implemented. FCM integration required."),
        )
    }
}
