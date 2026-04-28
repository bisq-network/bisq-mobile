package network.bisq.mobile.data.crypto

import android.util.Base64
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the rotation / read semantics. The actual
 * `EncryptedSharedPreferences` storage is swapped for an in-memory fake because
 * `AndroidKeyStore` does not work under Robolectric — Tink's master-key
 * generation fails. Production storage is exercised by instrumented tests
 * outside of unit-test scope.
 */
@RunWith(RobolectricTestRunner::class)
class PushNotificationKeyAndroidTest {
    private val fakeStore = InMemoryKeyStore()
    private val originalFactory = pushNotificationKeyStoreFactory

    @Before
    fun setUp() {
        pushNotificationKeyStoreFactory = { fakeStore }
    }

    @After
    fun tearDown() {
        pushNotificationKeyStoreFactory = originalFactory
    }

    @Test
    fun `getOrCreate generates a 32-byte AES-256 key`() {
        val base64 = getOrCreatePushNotificationKeyBase64()

        assertNotNull(base64, "expected a Base64 key, got null")
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        assertEquals(32, bytes.size, "AES-256 requires 32 bytes")
    }

    @Test
    fun `getOrCreate rotates the key on every call (mirrors iOS behaviour)`() {
        val first = getOrCreatePushNotificationKeyBase64()
        val second = getOrCreatePushNotificationKeyBase64()

        assertNotNull(first)
        assertNotNull(second)
        assertNotEquals(first, second, "each call must rotate to a fresh key")
    }

    @Test
    fun `read returns the most recently stored key`() {
        val written = getOrCreatePushNotificationKeyBase64()
        val read = readPushNotificationKeyBase64()

        assertEquals(written, read)
    }

    @Test
    fun `read returns null when no key has been stored yet`() {
        val read = readPushNotificationKeyBase64()
        assertNull(read)
    }

    @Test
    fun `key is encoded with NO_WRAP (no embedded newlines)`() {
        val base64 = getOrCreatePushNotificationKeyBase64()
        assertNotNull(base64)
        assertTrue(!base64.contains('\n'), "Base64 must not contain newlines: $base64")
    }

    @Test
    fun `getOrCreate returns null when the underlying store fails to persist`() {
        // Simulates SharedPreferences.commit() returning false (or any other
        // write failure). The outer runCatching in getOrCreatePushNotificationKeyBase64
        // must convert this into a null return — callers (validateSymmetricKey)
        // then abort registration before the trusted node receives a key the
        // device can't actually decrypt with.
        pushNotificationKeyStoreFactory = { ThrowingKeyStore() }

        val base64 = getOrCreatePushNotificationKeyBase64()

        assertNull(base64)
    }

    @Test
    fun `read returns null when the underlying store throws`() {
        pushNotificationKeyStoreFactory = { ThrowingKeyStore() }

        val read = readPushNotificationKeyBase64()

        assertNull(read)
    }

    private class InMemoryKeyStore : PushNotificationKeyStore {
        private var stored: String? = null

        override fun put(base64: String) {
            stored = base64
        }

        override fun get(): String? = stored
    }

    private class ThrowingKeyStore : PushNotificationKeyStore {
        override fun put(base64: String): Unit = throw IllegalStateException("Failed to persist push notification symmetric key")

        override fun get(): String? = throw IllegalStateException("Failed to read")
    }
}
