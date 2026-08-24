package network.bisq.mobile.data.crypto

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Deletes with the migration it covers (see `LegacyPushNotificationKeyCleanup`).
 * The Keystore half of the cleanup, dropping the orphaned master key, only runs on
 * a device and is covered by `LegacyPushNotificationKeyCleanupInstrumentedTest`.
 */
@RunWith(RobolectricTestRunner::class)
class LegacyPushNotificationKeyCleanupTest {
    private companion object {
        const val LEGACY_PREFS_FILE = "bisq_push_notification_keystore"
        const val LEGACY_PREF_KEY = "push_notification_symmetric_key_base64"
    }

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val legacyPrefs get() = context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        legacyPrefs.edit().clear().commit()
    }

    @Test
    fun `removes the key material the legacy store held`() {
        legacyPrefs
            .edit()
            .putString(LEGACY_PREF_KEY, "legacy")
            .commit()

        deleteLegacyPushNotificationStore(context)

        assertNull(legacyPrefs.getString(LEGACY_PREF_KEY, null))
    }

    @Test
    fun `leaves an install that never held the legacy store untouched`() {
        deleteLegacyPushNotificationStore(context)

        assertEquals(0, legacyPrefs.all.size)
    }

    @Test
    fun `runs only once per process`() {
        deleteLegacyPushNotificationStoreOnce(context)
        legacyPrefs
            .edit()
            .putString(LEGACY_PREF_KEY, "written after the one-shot cleanup")
            .commit()

        deleteLegacyPushNotificationStoreOnce(context)

        assertEquals("written after the one-shot cleanup", legacyPrefs.getString(LEGACY_PREF_KEY, null))
    }
}
