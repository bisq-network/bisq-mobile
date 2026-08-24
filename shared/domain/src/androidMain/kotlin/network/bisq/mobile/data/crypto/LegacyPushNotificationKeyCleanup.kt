package network.bisq.mobile.data.crypto

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean

// Temporary: this file exists only to retire the storage that shipped up to 0.8.2 and
// can be deleted once every install has launched a build containing the migration.
// Removing it means dropping this file, its two tests (LegacyPushNotificationKeyCleanupTest
// and LegacyPushNotificationKeyCleanupInstrumentedTest), the call in SharedPrefsKeyStore's
// init block and the LegacyMasterKey entry in KoverExclusions.

// Artifacts of the pre-Keystore-wrap implementation, which stored the key in
// `EncryptedSharedPreferences` (androidx.security-crypto, deprecated in 1.1.0).
// The Tink keyset lived inside the prefs file itself, so deleting the file
// removes it; the master key is a separate AndroidKeyStore entry. Nothing else
// in the app uses androidx.security-crypto, so both are safe to drop.
private const val LEGACY_PREFS_FILE = "bisq_push_notification_keystore"
private const val LEGACY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"

private val legacyStoreCleaned = AtomicBoolean(false)

/**
 * Drops the `EncryptedSharedPreferences` store used before the Keystore-wrap
 * migration. The old key is deliberately not carried over: every registration
 * rotates the key anyway, and the first `activate()` after the upgrade
 * re-registers a fresh one with the trusted node. Pushes that arrive in between
 * are dropped undecrypted, which is the same outcome as any other rotation gap.
 *
 * Runs once per process; repeat calls are a no-op.
 */
internal fun deleteLegacyPushNotificationStoreOnce(context: Context) {
    if (legacyStoreCleaned.compareAndSet(false, true)) {
        deleteLegacyPushNotificationStore(context)
    }
}

/**
 * The cleanup itself, without the once-per-process guard. Production goes through
 * [deleteLegacyPushNotificationStoreOnce]; tests call this directly.
 *
 * Best-effort by design, because a Keystore that refuses to drop an orphan alias must
 * not break key storage. The master key is only touched when the legacy prefs file was
 * actually there: that alias is androidx.security's shared default, so on a device
 * that never held our old store it may well belong to something else, and deleting
 * it would destroy that owner's data with no way to recover it.
 */
@VisibleForTesting
internal fun deleteLegacyPushNotificationStore(context: Context) {
    val hadLegacyStore =
        runCatching {
            val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)
            // Reading the entries back is the dependable "did this device hold the old
            // store?" check: getSharedPreferences never creates the file on its own, while
            // deleteSharedPreferences reports deletion of a backup file that normally does
            // not exist, so its return value says nothing useful.
            val hadEntries = legacyPrefs.all.isNotEmpty()
            if (hadEntries) {
                // Clear first: this is what actually removes the key material. Dropping the
                // file afterwards is cosmetic, so a platform that refuses it changes nothing.
                legacyPrefs.edit().clear().commit()
                context.deleteSharedPreferences(LEGACY_PREFS_FILE)
            }
            hadEntries
        }.getOrDefault(false)
    if (!hadLegacyStore) return

    LegacyMasterKey.deleteIfPresent()
}

/**
 * The Keystore half of the cleanup, kept as its own type because Robolectric ships no
 * `AndroidKeyStore` provider: these lines only ever run on a device, and the coverage
 * gate excludes them by class name.
 */
private object LegacyMasterKey {
    fun deleteIfPresent() {
        runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(LEGACY_MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(LEGACY_MASTER_KEY_ALIAS)
            }
        }
    }
}
