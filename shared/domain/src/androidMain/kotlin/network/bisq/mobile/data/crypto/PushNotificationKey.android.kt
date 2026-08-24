package network.bisq.mobile.data.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import network.bisq.mobile.data.utils.AndroidAppContext
import network.bisq.mobile.domain.utils.getLogger
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val log = getLogger("PushNotificationKey")

private const val KEY_SIZE_BYTES = 32 // AES-256
private const val PREFS_FILE = "bisq_push_notification_key"
private const val PREF_KEY_WRAPPED = "wrapped_symmetric_key_base64"
private const val WRAPPING_KEY_ALIAS = "network.bisq.mobile.push_notification_key_wrapper"

/**
 * Read/write port for the push notification symmetric key. Production swaps in
 * a Keystore-wrapped implementation; tests can swap in an in-memory fake to
 * bypass `AndroidKeyStore` (which Robolectric can't fully emulate).
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
    SharedPrefsKeyStore(AndroidAppContext.context)
}

/**
 * Rotates and returns the AES-256 symmetric key for push notification encryption.
 * A fresh key is generated on every call to limit the exposure window if a key
 * is ever compromised — this matches the iOS Keychain rotation behaviour
 * (see `iosClient/iosClient/interop/PushNotificationKeyStore.swift`).
 *
 * The key is wrapped with an AES-GCM key held in the Android Keystore and the
 * resulting blob is kept in plain SharedPreferences. The Base64-encoded key is
 * returned to the caller so it can be sent to the trusted node, which then
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
    }.onFailure {
        // Callers only see a null and abort registration, so without this the reason
        // (Keystore refusal, failed commit) never reaches the logs. The exceptions carry
        // no key material, so they are safe to log in full.
        log.e(it) { "Failed to rotate the push notification key" }
    }.getOrNull()

/**
 * Reads the most recently stored symmetric key, used by `BisqFirebaseMessagingService`
 * to decrypt incoming pushes. Returns `null` if no key has been generated yet
 * (i.e. the user has not opted in / registered for push notifications) or if the
 * stored blob can no longer be unwrapped. A Keystore that lost or invalidated the
 * wrapping key has to degrade to "no key" (drop the push) rather than crash the
 * messaging service. The next registration rotates into a fresh, usable key.
 */
fun readPushNotificationKeyBase64(): String? =
    runCatching {
        pushNotificationKeyStoreFactory().get()
    }.onFailure {
        // Distinguishes "never registered" from "the Keystore blew up" in the field, which
        // the messaging service cannot tell apart once this returns null.
        log.e(it) { "Failed to read the push notification key; treating it as absent" }
    }.getOrNull()

/**
 * Wraps bytes with a non-exportable key so the result can live in plain storage.
 * Production goes through the Android Keystore; unit tests substitute a trivial
 * transform because Robolectric cannot emulate `AndroidKeyStore`.
 *
 * "Wrap" here is the key-encryption-key sense, one key protecting another. It is not
 * `Cipher.WRAP_MODE` / `KeyProperties.PURPOSE_WRAP_KEY`, which exist for Keystore's
 * secure key import and are not involved.
 */
internal interface PushNotificationKeyWrapper {
    fun wrap(bytes: ByteArray): ByteArray

    fun unwrap(bytes: ByteArray): ByteArray
}

/**
 * AES-GCM through [LocalEncryption], the Keystore helper the sensitive-settings
 * storage also uses. Same code path, but under its own alias, so the wrapping key
 * is not shared with any other stored data.
 */
private object KeystoreKeyWrapper : PushNotificationKeyWrapper {
    override fun wrap(bytes: ByteArray): ByteArray = LocalEncryption.encrypt(bytes, WRAPPING_KEY_ALIAS)

    override fun unwrap(bytes: ByteArray): ByteArray = LocalEncryption.decrypt(bytes, WRAPPING_KEY_ALIAS)
}

/**
 * Keeps the key as `Base64(iv || ciphertext)` in plain SharedPreferences, wrapped
 * with a non-exportable Keystore key.
 *
 * SharedPreferences rather than DataStore because [put] must be durable before the
 * caller registers the key with the trusted node (see the `commit()` note below);
 * DataStore is async-first and would need `runBlocking` to offer the same guarantee.
 */
internal class SharedPrefsKeyStore(
    context: Context,
    private val wrapper: PushNotificationKeyWrapper = KeystoreKeyWrapper,
) : PushNotificationKeyStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    init {
        deleteLegacyPushNotificationStoreOnce(context)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun put(base64: String) {
        val wrapped = Base64.encode(wrapper.wrap(base64.toByteArray(Charsets.UTF_8)))
        // commit() (synchronous) rather than apply() (async): the symmetric
        // key is registered with the trusted node immediately after this
        // returns. If apply() were used and the process died before the
        // write hit disk, the server and device would diverge on the key
        // and decryption would silently fail.
        val ok = prefs.edit().putString(PREF_KEY_WRAPPED, wrapped).commit()
        check(ok) { "Failed to persist push notification symmetric key" }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun get(): String? {
        val wrapped = prefs.getString(PREF_KEY_WRAPPED, null) ?: return null
        return wrapper.unwrap(Base64.decode(wrapped)).toString(Charsets.UTF_8)
    }
}
