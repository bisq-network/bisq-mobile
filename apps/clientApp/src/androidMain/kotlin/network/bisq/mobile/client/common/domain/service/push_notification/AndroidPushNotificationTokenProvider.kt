package network.bisq.mobile.client.common.domain.service.push_notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import network.bisq.mobile.data.utils.AndroidAppContext
import network.bisq.mobile.domain.utils.Logging

/**
 * Android FCM implementation of [PushNotificationTokenProvider].
 *
 * Permission flow: this provider only reports whether the runtime
 * `POST_NOTIFICATIONS` permission has been granted (Android 13+). Triggering
 * the system permission prompt requires an Activity context — that part is
 * handled by the UI layer (Settings screen toggle) before this is called.
 */
class AndroidPushNotificationTokenProvider :
    PushNotificationTokenProvider,
    Logging {
    override suspend fun requestPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Pre-Android 13: notifications don't require runtime permission.
            return true
        }
        val granted =
            ContextCompat.checkSelfPermission(
                AndroidAppContext.context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            log.w {
                "POST_NOTIFICATIONS permission not granted. The Settings UI " +
                    "must request it from an Activity before registering."
            }
        }
        return granted
    }

    override suspend fun requestDeviceToken(): Result<String> =
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            check(token.isNotBlank()) { "FCM returned a blank token" }
            log.i { "Got FCM token: ${token.take(10)}..." }
            token
        }.recoverCatching { throwable ->
            log.e(throwable) { "Failed to fetch FCM token" }
            throw PushNotificationException(
                "Failed to fetch FCM token. Verify google-services.json is configured.",
                throwable,
            )
        }
}
