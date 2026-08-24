package network.bisq.mobile.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.data.crypto.getOrCreatePushNotificationKeyBase64
import network.bisq.mobile.data.crypto.readPushNotificationKeyBase64
import network.bisq.mobile.data.utils.AndroidAppContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the production (Android Keystore backed) push notification key store,
 * which the Robolectric unit tests have to stub out. The storage names are spelled
 * out here rather than imported so this test also pins the on-disk file name that
 * `clientApp/res/xml/data_extraction_rules.xml` documents as backup-excluded.
 */
@RunWith(AndroidJUnit4::class)
class PushNotificationKeyStoreInstrumentedTest {
    private companion object {
        const val PREFS_FILE = "bisq_push_notification_key"
        const val PREF_KEY_WRAPPED = "wrapped_symmetric_key_base64"
        const val WRAPPING_KEY_ALIAS = "network.bisq.mobile.push_notification_key_wrapper"
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        AndroidAppContext.reset()
        AndroidAppContext.initialize(context)
        clearStore()
    }

    @After
    fun tearDown() {
        clearStore()
        AndroidAppContext.reset()
    }

    @Test
    fun rotatedKeyIsReadBack() {
        val written = getOrCreatePushNotificationKeyBase64()

        assertNotNull(written)
        assertEquals(written, readPushNotificationKeyBase64())
    }

    @Test
    fun rotationOverwritesThePreviousKey() {
        val first = getOrCreatePushNotificationKeyBase64()
        val second = getOrCreatePushNotificationKeyBase64()

        assertNotEquals(first, second)
        assertEquals(second, readPushNotificationKeyBase64())
    }

    @Test
    fun keyIsNotStoredInPlaintext() {
        val written = getOrCreatePushNotificationKeyBase64()
        assertNotNull(written)

        val stored = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).getString(PREF_KEY_WRAPPED, null)

        assertNotNull(stored, "expected a wrapped blob on disk")
        assertNotEquals(written, stored)
        assertTrue(!stored.contains(written), "the raw key must not appear in the stored blob")
    }

    @Test
    fun readReturnsNullWhenNothingWasStored() {
        assertNull(readPushNotificationKeyBase64())
    }

    @Test
    fun corruptBlobDegradesToNoKey() {
        getOrCreatePushNotificationKeyBase64()
        context
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_WRAPPED, "not-a-wrapped-key")
            .commit()

        assertNull(readPushNotificationKeyBase64())
    }

    @Test
    fun lostWrappingKeyDegradesToNoKeyAndRecoversOnRotation() {
        getOrCreatePushNotificationKeyBase64()
        // Simulates the Keystore dropping / invalidating the wrapping key (OEM quirks,
        // lock-screen changes): the stored blob becomes undecryptable.
        deleteWrappingKey()

        assertNull(readPushNotificationKeyBase64())

        val rotated = getOrCreatePushNotificationKeyBase64()
        assertNotNull(rotated)
        assertEquals(rotated, readPushNotificationKeyBase64())
    }

    private fun clearStore() {
        context
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        deleteWrappingKey()
    }

    private fun deleteWrappingKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(WRAPPING_KEY_ALIAS)) {
            keyStore.deleteEntry(WRAPPING_KEY_ALIAS)
        }
    }
}
