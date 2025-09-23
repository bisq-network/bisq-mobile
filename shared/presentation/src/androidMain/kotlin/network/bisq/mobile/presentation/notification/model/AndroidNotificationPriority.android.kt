package network.bisq.mobile.presentation.notification.model

import androidx.core.app.NotificationCompat


/**
 * Converts the KMP enum value to the target platform value
 */
fun AndroidNotificationPriority?.toNotificationCompat(): Int {
    return when(this) {
        AndroidNotificationPriority.PRIORITY_MIN -> NotificationCompat.PRIORITY_MIN
        AndroidNotificationPriority.PRIORITY_LOW -> NotificationCompat.PRIORITY_LOW
        AndroidNotificationPriority.PRIORITY_HIGH -> NotificationCompat.PRIORITY_HIGH
        AndroidNotificationPriority.PRIORITY_MAX -> NotificationCompat.PRIORITY_MAX
        else -> NotificationCompat.PRIORITY_DEFAULT
    }
}