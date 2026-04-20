package network.bisq.mobile.data.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import network.bisq.mobile.crypto.PushNotificationKeyStore

/**
 * Rotates and returns the AES-256 symmetric key for push notification encryption.
 * A fresh key is generated on each call (every app launch / re-registration) to limit
 * the exposure window if a key is ever compromised. The key is stored in a shared
 * Keychain access group accessible by both the main app and the NSE.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getOrCreatePushNotificationKeyBase64(): String? = PushNotificationKeyStore.Companion.shared().rotateKeyBase64WithError(null)
