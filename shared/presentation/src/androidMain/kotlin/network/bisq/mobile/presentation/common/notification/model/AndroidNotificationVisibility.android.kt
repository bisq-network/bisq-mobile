package network.bisq.mobile.presentation.common.notification.model

import androidx.core.app.NotificationCompat
import network.bisq.mobile.presentation.common.notification.model.android.AndroidNotificationVisibility

fun AndroidNotificationVisibility.toNotificationCompat(): Int =
    when (this) {
        AndroidNotificationVisibility.VISIBILITY_PUBLIC -> NotificationCompat.VISIBILITY_PUBLIC
        AndroidNotificationVisibility.VISIBILITY_PRIVATE -> NotificationCompat.VISIBILITY_PRIVATE
        AndroidNotificationVisibility.VISIBILITY_SECRET -> NotificationCompat.VISIBILITY_SECRET
    }
