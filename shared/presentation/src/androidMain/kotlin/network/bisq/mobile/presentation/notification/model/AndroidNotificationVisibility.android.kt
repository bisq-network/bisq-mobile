package network.bisq.mobile.presentation.notification.model

import androidx.core.app.NotificationCompat

fun AndroidNotificationVisibility.toNotificationCompat(): Int {
    return when(this) {
        AndroidNotificationVisibility.VISIBILITY_PUBLIC -> NotificationCompat.VISIBILITY_PUBLIC
        AndroidNotificationVisibility.VISIBILITY_PRIVATE -> NotificationCompat.VISIBILITY_PRIVATE
        AndroidNotificationVisibility.VISIBILITY_SECRET -> NotificationCompat.VISIBILITY_SECRET
    }
}