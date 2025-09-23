package network.bisq.mobile.presentation.notification.model

import androidx.core.app.NotificationCompat


/**
 * Converts the KMP enum value to the target platform value
 */
fun AndroidNotificationCategory?.toNotificationCompat(): String {
    return when(this) {
        AndroidNotificationCategory.CATEGORY_CALL -> NotificationCompat.CATEGORY_CALL
        AndroidNotificationCategory.CATEGORY_NAVIGATION -> NotificationCompat.CATEGORY_NAVIGATION
        AndroidNotificationCategory.CATEGORY_MESSAGE -> NotificationCompat.CATEGORY_MESSAGE
        AndroidNotificationCategory.CATEGORY_EMAIL -> NotificationCompat.CATEGORY_EMAIL
        AndroidNotificationCategory.CATEGORY_EVENT -> NotificationCompat.CATEGORY_EVENT
        AndroidNotificationCategory.CATEGORY_PROMO -> NotificationCompat.CATEGORY_PROMO
        AndroidNotificationCategory.CATEGORY_ALARM -> NotificationCompat.CATEGORY_ALARM
        AndroidNotificationCategory.CATEGORY_PROGRESS -> NotificationCompat.CATEGORY_PROGRESS
        AndroidNotificationCategory.CATEGORY_SOCIAL -> NotificationCompat.CATEGORY_SOCIAL
        AndroidNotificationCategory.CATEGORY_ERROR -> NotificationCompat.CATEGORY_ERROR
        AndroidNotificationCategory.CATEGORY_TRANSPORT -> NotificationCompat.CATEGORY_TRANSPORT
        AndroidNotificationCategory.CATEGORY_SYSTEM -> NotificationCompat.CATEGORY_SYSTEM
        AndroidNotificationCategory.CATEGORY_SERVICE -> NotificationCompat.CATEGORY_SERVICE
        AndroidNotificationCategory.CATEGORY_REMINDER -> NotificationCompat.CATEGORY_REMINDER
        AndroidNotificationCategory.CATEGORY_RECOMMENDATION -> NotificationCompat.CATEGORY_RECOMMENDATION
        AndroidNotificationCategory.CATEGORY_STATUS  -> NotificationCompat.CATEGORY_STATUS
        AndroidNotificationCategory.CATEGORY_WORKOUT  -> NotificationCompat.CATEGORY_WORKOUT
        AndroidNotificationCategory.CATEGORY_LOCATION_SHARING  -> NotificationCompat.CATEGORY_LOCATION_SHARING
        AndroidNotificationCategory.CATEGORY_STOPWATCH  -> NotificationCompat.CATEGORY_STOPWATCH
        AndroidNotificationCategory.CATEGORY_MISSED_CALL  -> NotificationCompat.CATEGORY_MISSED_CALL
        AndroidNotificationCategory.CATEGORY_VOICEMAIL  -> NotificationCompat.CATEGORY_VOICEMAIL
        null -> NotificationCompat.CATEGORY_MESSAGE
    }
}