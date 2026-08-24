package network.bisq.mobile.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.data.crypto.deleteLegacyPushNotificationStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.crypto.KeyGenerator
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the Keystore half of the migration, which Robolectric cannot reach. Deletes
 * with the migration it belongs to (see `LegacyPushNotificationKeyCleanup`).
 *
 * The master key alias is androidx.security's shared default, hence the second test:
 * dropping it on an install that never held our legacy store would destroy key material
 * belonging to whatever else adopted that alias.
 */
@RunWith(AndroidJUnit4::class)
class LegacyPushNotificationKeyCleanupInstrumentedTest {
    private companion object {
        const val LEGACY_PREFS_FILE = "bisq_push_notification_keystore"
        const val LEGACY_PREF_KEY = "push_notification_symmetric_key_base64"
        const val LEGACY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val legacyPrefs get() = context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        legacyPrefs.edit().clear().commit()
        deleteMasterKey()
    }

    @After
    fun tearDown() {
        legacyPrefs.edit().clear().commit()
        deleteMasterKey()
    }

    @Test
    fun dropsTheLegacyEntriesAndTheOrphanedMasterKey() {
        legacyPrefs
            .edit()
            .putString(LEGACY_PREF_KEY, "legacy")
            .commit()
        createMasterKey()

        deleteLegacyPushNotificationStore(context)

        assertNull(legacyPrefs.getString(LEGACY_PREF_KEY, null))
        assertFalse(keyStore().containsAlias(LEGACY_MASTER_KEY_ALIAS), "orphaned master key should be gone")
    }

    @Test
    fun keepsTheMasterKeyWhenThisInstallNeverHeldTheLegacyStore() {
        createMasterKey()

        deleteLegacyPushNotificationStore(context)

        assertTrue(
            keyStore().containsAlias(LEGACY_MASTER_KEY_ALIAS),
            "the shared androidx.security alias must survive on installs without our legacy store",
        )
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun createMasterKey() {
        KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec
                        .Builder(
                            LEGACY_MASTER_KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build(),
                )
            }.generateKey()
    }

    private fun deleteMasterKey() {
        val keyStore = keyStore()
        if (keyStore.containsAlias(LEGACY_MASTER_KEY_ALIAS)) {
            keyStore.deleteEntry(LEGACY_MASTER_KEY_ALIAS)
        }
    }
}
