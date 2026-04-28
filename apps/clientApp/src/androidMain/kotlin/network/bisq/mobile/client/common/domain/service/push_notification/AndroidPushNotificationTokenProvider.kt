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
 * Privacy posture: Firebase auto-init is OFF by default
 * (see `AndroidManifest.xml` meta-data). This provider flips
 * `isAutoInitEnabled = true` only when the user explicitly registers, and
 * back to `false` on unregister. As a result, devices that never opt in to
 * push notifications never establish a connection to Google's servers, even
 * though the FCM library is bundled in the APK.
 *
 * Permission flow: [requestPermission] only reports whether the runtime
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
            // User has opted in — turn Firebase on. Until this line runs,
            // no FCM connection to Google's servers is established.
            FirebaseMessaging.getInstance().isAutoInitEnabled = true

            val token = FirebaseMessaging.getInstance().token.await()
            check(token.isNotBlank()) { "FCM returned a blank token" }
            log.i { "Got FCM token: ${token.take(10)}..." }
            token
        }.recoverCatching { throwable ->
            log.e(throwable) { "Failed to fetch FCM token" }
            // Roll back auto-init on failure so we don't leave a half-on state.
            runCatching { FirebaseMessaging.getInstance().isAutoInitEnabled = false }
            throw PushNotificationException(
                "Failed to fetch FCM token. Verify google-services.json is configured.",
                throwable,
            )
        }

    override suspend fun revokeDeviceToken(): Result<Unit> =
        runCatching {
            log.i { "Revoking FCM token and disabling Firebase auto-init" }
            // deleteToken() unregisters the device on Google's side. We do this
            // first so the token is invalidated before we sever the connection.
            runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
                .onFailure { log.w(it) { "deleteToken() failed — continuing with auto-init off" } }
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
        }
}
