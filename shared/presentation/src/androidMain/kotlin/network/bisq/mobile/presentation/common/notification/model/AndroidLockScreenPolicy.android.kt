package network.bisq.mobile.presentation.common.notification.model

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import network.bisq.mobile.data.utils.ResourceUtils
import network.bisq.mobile.presentation.common.notification.model.android.AndroidLockScreenPolicy

// VISIBILITY_SECRET has no policy mapping to it on purpose: nothing produces one today, and a
// variant nobody constructs works against the point of modelling this as a closed set. Add it here
// alongside the producer when something needs a notification hidden outright.
fun AndroidLockScreenPolicy.toNotificationCompat(): Int =
    when (this) {
        is AndroidLockScreenPolicy.ShowContent -> NotificationCompat.VISIBILITY_PUBLIC
        is AndroidLockScreenPolicy.Redact -> NotificationCompat.VISIBILITY_PRIVATE
    }

/**
 * The stand-in the SystemUI draws on a secure lock screen in place of the real notification.
 *
 * Deliberately bare: no content intent, no group, no sound — it is only ever rendered, never
 * interacted with. It must itself be PUBLIC, or there would be nothing left to fall back to.
 *
 * Both notification paths build one. `NotificationControllerImpl` takes the policy from the `notify {}`
 * DSL; `BisqFirebaseMessagingService` takes it off the `PushNotification` variant a decrypted push
 * collapses into. So the same event redacts to the same copy whether the app raised it locally or the
 * relay delivered it — which is the whole reason the stand-in copy lives in one place
 * (`NotificationRedactions`) rather than at each producer.
 */
fun AndroidLockScreenPolicy.Redact.toPublicNotification(
    context: Context,
    channelId: String,
): Notification =
    NotificationCompat
        .Builder(context, channelId)
        .setSmallIcon(ResourceUtils.getNotifResId(context))
        .setContentTitle(title)
        .setContentText(body)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()
