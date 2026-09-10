package network.bisq.mobile.data.crypto

import android.content.Context
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Covers the storage half of the production key store (persistence and encoding)
 * with the Keystore wrapping swapped for a reversible stand-in,
 * since Robolectric cannot emulate `AndroidKeyStore`. The real wrapping is covered
 * by `PushNotificationKeyStoreInstrumentedTest`.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalEncodingApi::class)
class SharedPrefsKeyStoreTest {
    private companion object {
        const val PREFS_FILE = "bisq_push_notification_key"
        const val PREF_KEY_WRAPPED = "wrapped_symmetric_key_base64"
        const val KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
        const val KEY2 = "ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8="
        const val KEY3 = "QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xdXl8="
    }

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val originalFactory = pushNotificationKeyStoreFactory

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
    }

    @After
    fun tearDown() {
        pushNotificationKeyStoreFactory = originalFactory
    }

    @Test
    fun `round trips the key through wrap and unwrap`() {
        val store = SharedPrefsKeyStore(context, XorWrapper)

        store.put(KEY)

        assertEquals(KEY, store.get())
    }

    @Test
    fun `there is no previous generation before the first rotation`() {
        val store = SharedPrefsKeyStore(context, XorWrapper)

        store.put(KEY)

        assertNull(store.getPrevious())
    }

    /**
     * The displaced key must stay readable for one generation: a push encrypted just before a
     * rotation (or queued by FCM while the app was closed) arrives under it, and dropping it on
     * rotation is exactly the notification loss the two-key window exists to prevent.
     */
    @Test
    fun `rotation keeps the displaced key readable as the previous generation`() {
        val store = SharedPrefsKeyStore(context, XorWrapper)

        store.put(KEY)
        store.put(KEY2)

        assertEquals(KEY2, store.get())
        assertEquals(KEY, store.getPrevious())
    }

    @Test
    fun `a second rotation shifts the one-generation window`() {
        val store = SharedPrefsKeyStore(context, XorWrapper)

        store.put(KEY)
        store.put(KEY2)
        store.put(KEY3)

        assertEquals(KEY3, store.get())
        assertEquals(KEY2, store.getPrevious())
    }

    @Test
    fun `the previous generation is never written in plaintext`() {
        val store = SharedPrefsKeyStore(context, XorWrapper)

        store.put(KEY)
        store.put(KEY2)

        val allValues = prefs.all.values.joinToString()
        assertFalse(allValues.contains(KEY), "displaced key must stay wrapped at rest")
        assertFalse(allValues.contains(KEY2), "current key must stay wrapped at rest")
    }

    @Test
    fun `never writes the key in plaintext`() {
        SharedPrefsKeyStore(context, XorWrapper).put(KEY)

        val stored = prefs.getString(PREF_KEY_WRAPPED, null)

        assertNotNull(stored)
        assertNotEquals(KEY, stored)
        assertNotEquals(KEY.toByteArray().toList(), Base64.decode(stored).toList())
    }

    @Test
    fun `a second put replaces the previous key`() {
        val store = SharedPrefsKeyStore(context, XorWrapper)
        val other = "f".repeat(KEY.length)

        store.put(KEY)
        store.put(other)

        assertEquals(other, store.get())
    }

    @Test
    fun `get returns null when nothing was stored`() {
        assertNull(SharedPrefsKeyStore(context, XorWrapper).get())
    }

    @Test
    fun `a corrupt blob surfaces as no key rather than a crash`() {
        prefs
            .edit()
            .putString(PREF_KEY_WRAPPED, "not a wrapped key")
            .commit()
        pushNotificationKeyStoreFactory = { SharedPrefsKeyStore(context, XorWrapper) }

        assertNull(readPushNotificationKeyBase64())
    }

    @Test
    fun `an unusable wrapping key surfaces as no key rather than a crash`() {
        SharedPrefsKeyStore(context, XorWrapper).put(KEY)
        // Stands in for a Keystore that lost or invalidated the wrapping key.
        val store = SharedPrefsKeyStore(context, FailingWrapper)
        pushNotificationKeyStoreFactory = { store }

        assertFailsWith<IllegalStateException> { store.get() }
        assertNull(readPushNotificationKeyBase64())
    }

    /** Reversible stand-in for the Keystore: not encryption, just not the identity. */
    private object XorWrapper : PushNotificationKeyWrapper {
        override fun wrap(bytes: ByteArray): ByteArray = ByteArray(bytes.size) { (bytes[it].toInt() xor 0x5A).toByte() }

        override fun unwrap(bytes: ByteArray): ByteArray = wrap(bytes)
    }

    private object FailingWrapper : PushNotificationKeyWrapper {
        override fun wrap(bytes: ByteArray): ByteArray = throw IllegalStateException("Keystore unavailable")

        override fun unwrap(bytes: ByteArray): ByteArray = throw IllegalStateException("Keystore unavailable")
    }
}
