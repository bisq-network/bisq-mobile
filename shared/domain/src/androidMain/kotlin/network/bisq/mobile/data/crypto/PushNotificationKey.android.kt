package network.bisq.mobile.data.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import network.bisq.mobile.data.utils.AndroidAppContext
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val KEY_SIZE_BYTES = 32 // AES-256
private const val PREFS_FILE = "bisq_push_notification_keystore"
private const val PREF_KEY_SYMMETRIC = "push_notification_symmetric_key_base64"

// TODO(follow-up): migrate from EncryptedSharedPreferences/MasterKey
// (deprecated in androidx.security-crypto 1.1.0) to direct Android Keystore
// APIs. The wrapper still works but Google now recommends generating an AES
// key in Keystore and using it to wrap/unwrap the per-device symmetric key
// stored in plain SharedPreferences. Tracked as a nitpick — the deprecated
// APIs remain functional in 1.1.0.

/**
 * Read/write port for the push notification symmetric key. Production swaps in
 * an `EncryptedSharedPreferences`-backed implementation; tests can swap in an
 * in-memory fake to bypass `AndroidKeyStore` (which Robolectric can't fully
 * emulate — Tink's master-key generation fails in unit tests).
 */
@VisibleForTesting
interface PushNotificationKeyStore {
    fun put(base64: String)

    fun get(): String?
}

/**
 * Seam for tests in modules other than `:shared:domain` to swap in a fake
 * key store (`internal` would block them via Kotlin module visibility).
 * Production code keeps the default factory; nothing else should mutate it.
 */
@VisibleForTesting
var pushNotificationKeyStoreFactory: () -> PushNotificationKeyStore = {
    EncryptedSharedPrefsKeyStore(AndroidAppContext.context)
}

/**
 * Rotates and returns the AES-256 symmetric key for push notification encryption.
 * A fresh key is generated on every call to limit the exposure window if a key
 * is ever compromised — this matches the iOS Keychain rotation behaviour
 * (see `iosClient/iosClient/interop/PushNotificationKeyStore.swift`).
 *
 * The key is stored in `EncryptedSharedPreferences`, whose contents are encrypted
 * at rest with a `MasterKey` held in the Android Keystore. The Base64-encoded key
 * is returned to the caller so it can be sent to the trusted node, which then
 * encrypts notification payloads with AES-256-GCM that this device decrypts in
 * its `FirebaseMessagingService`.
 */
@OptIn(ExperimentalEncodingApi::class)
actual fun getOrCreatePushNotificationKeyBase64(): String? =
    runCatching {
        val store = pushNotificationKeyStoreFactory()
        val keyBytes = ByteArray(KEY_SIZE_BYTES)
        SecureRandom().nextBytes(keyBytes)
        // Kotlin stdlib Base64 (works on plain JVM and Android — `android.util.Base64`
        // returns null in non-Robolectric unit tests, so we use the stdlib variant
        // for portability).
        val base64 = Base64.encode(keyBytes)
        store.put(base64)
        base64
    }.getOrNull()

/**
 * Reads the most recently stored symmetric key, used by `BisqFirebaseMessagingService`
 * to decrypt incoming pushes. Returns `null` if no key has been generated yet
 * (i.e. the user has not opted in / registered for push notifications).
 */
fun readPushNotificationKeyBase64(): String? =
    runCatching {
        pushNotificationKeyStoreFactory().get()
    }.getOrNull()

private class EncryptedSharedPrefsKeyStore(
    context: Context,
) : PushNotificationKeyStore {
    private val prefs: SharedPreferences =
        run {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

    override fun put(base64: String) {
        // commit() (synchronous) rather than apply() (async): the symmetric
        // key is registered with the trusted node immediately after this
        // returns. If apply() were used and the process died before the
        // write hit disk, the server and device would diverge on the key
        // and decryption would silently fail.
        val ok = prefs.edit().putString(PREF_KEY_SYMMETRIC, base64).commit()
        check(ok) { "Failed to persist push notification symmetric key" }
    }

    override fun get(): String? = prefs.getString(PREF_KEY_SYMMETRIC, null)
}
