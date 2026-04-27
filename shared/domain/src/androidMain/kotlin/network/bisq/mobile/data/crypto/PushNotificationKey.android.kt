package network.bisq.mobile.data.crypto

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import network.bisq.mobile.data.utils.AndroidAppContext
import java.security.SecureRandom

private const val KEY_SIZE_BYTES = 32 // AES-256
private const val PREFS_FILE = "bisq_push_notification_keystore"
private const val PREF_KEY_SYMMETRIC = "push_notification_symmetric_key_base64"

/**
 * Read/write port for the push notification symmetric key. Production swaps in
 * an `EncryptedSharedPreferences`-backed implementation; tests can swap in an
 * in-memory fake to bypass `AndroidKeyStore` (which Robolectric can't fully
 * emulate — Tink's master-key generation fails in unit tests).
 */
internal interface PushNotificationKeyStore {
    fun put(base64: String)

    fun get(): String?
}

@VisibleForTesting
internal var pushNotificationKeyStoreFactory: () -> PushNotificationKeyStore = {
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
actual fun getOrCreatePushNotificationKeyBase64(): String? =
    runCatching {
        val store = pushNotificationKeyStoreFactory()
        val keyBytes = ByteArray(KEY_SIZE_BYTES)
        SecureRandom().nextBytes(keyBytes)
        val base64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
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
        prefs.edit().putString(PREF_KEY_SYMMETRIC, base64).apply()
    }

    override fun get(): String? = prefs.getString(PREF_KEY_SYMMETRIC, null)
}
