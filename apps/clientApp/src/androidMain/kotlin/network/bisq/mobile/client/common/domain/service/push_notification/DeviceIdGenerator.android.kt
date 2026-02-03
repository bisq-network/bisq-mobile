package network.bisq.mobile.client.common.domain.service.push_notification

import android.provider.Settings
import network.bisq.mobile.presentation.main.ApplicationContextProvider

/**
 * Android implementation of getDeviceId().
 * Uses Settings.Secure.ANDROID_ID which:
 * - Is unique per device and user combination
 * - Persists across app reinstalls
 * - Changes on factory reset
 */
actual fun getDeviceId(): String {
    val context = ApplicationContextProvider.context
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    return androidId ?: throw IllegalStateException("Unable to get Android device ID")
}
