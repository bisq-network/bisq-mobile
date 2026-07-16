package network.bisq.mobile.client.common.domain.service.push_notification

import android.annotation.SuppressLint
import android.provider.Settings
import network.bisq.mobile.presentation.main.ApplicationContextProvider

/**
 * Android implementation of getDeviceId().
 * Uses Settings.Secure.ANDROID_ID which:
 * - Is unique per device and user combination
 * - Persists across app reinstalls
 * - Changes on factory reset
 *
 * ANDROID_ID here is a stable per-device key for push registration, NOT an advertising/analytics
 * identifier, so the lint HardwareIds guidance (AdvertisingIdClient / InstanceId) does not apply —
 * hence the @SuppressLint below.
 *
 * TODO: migrate deviceId to a self-generated, persisted random UUID (the API already accepts a
 *  "persisted UUID" — see PushNotificationApiGateway). It's more privacy-preserving (no device
 *  hardware id) and would fully remove the iOS transient-nil #1597 root cause. Needs a one-time
 *  migration for existing installs: read the old ANDROID_ID / identifierForVendor id, unregister
 *  it on the trusted node, then register the new UUID — otherwise old registrations orphan and
 *  users get duplicate pushes during the transition.
 */
@SuppressLint("HardwareIds")
actual fun getDeviceId(): String {
    val context = ApplicationContextProvider.context
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    return androidId ?: throw IllegalStateException("Unable to get Android device ID")
}
