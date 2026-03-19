package network.bisq.mobile.client.common.domain.sensitive_settings

import androidx.datastore.core.CorruptionException
import io.mockk.coEvery
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.crypto.decrypt
import network.bisq.mobile.crypto.encrypt
import okio.Buffer
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensitiveSettingsSerializerTest {
    private val decryptFqn = "network.bisq.mobile.crypto.LocalEncryption_androidKt"

    @Before
    fun setUp() {
        mockkStatic(decryptFqn)
        resetKeystoreInvalidatedFlag()
    }

    @After
    fun tearDown() {
        unmockkStatic(decryptFqn)
        resetKeystoreInvalidatedFlag()
    }

    private fun resetKeystoreInvalidatedFlag() {
        try {
            val field = SensitiveSettingsSerializer::class.java.getDeclaredField("_keystoreInvalidated")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (field.get(SensitiveSettingsSerializer) as MutableStateFlow<Boolean>).value = false
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun bufferWith(data: ByteArray): Buffer = Buffer().apply { write(data) }

    private fun mockDecryptThrows(exception: Throwable) {
        coEvery {
            decrypt(any(), any())
        } throws exception
    }

    private fun mockDecryptReturns(data: ByteArray) {
        coEvery {
            decrypt(any(), any())
        } returns data
    }

    @Test
    fun `readFrom returns default when source is exhausted`() =
        runTest {
            val result = SensitiveSettingsSerializer.readFrom(Buffer())
            assertEquals(SensitiveSettings(), result)
        }

    @Test
    fun `readFrom sets keystoreInvalidated when AEADBadTagException occurs`() =
        runTest {
            mockDecryptThrows(javax.crypto.AEADBadTagException("Tag mismatch"))

            assertFalse(SensitiveSettingsSerializer.keystoreInvalidated.value)

            assertFailsWith<CorruptionException> {
                SensitiveSettingsSerializer.readFrom(bufferWith(byteArrayOf(1, 2, 3)))
            }

            assertTrue(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }

    @Test
    fun `readFrom sets keystoreInvalidated when exception has AEADBadTagException in cause chain`() =
        runTest {
            val rootCause = javax.crypto.AEADBadTagException("Tag mismatch")
            mockDecryptThrows(RuntimeException("Decrypt failed", rootCause))

            assertFailsWith<CorruptionException> {
                SensitiveSettingsSerializer.readFrom(bufferWith(byteArrayOf(1, 2, 3)))
            }

            assertTrue(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }

    @Test
    fun `readFrom rethrows non-keystore exceptions without setting flag`() =
        runTest {
            mockDecryptThrows(RuntimeException("Some other error"))

            val thrown =
                assertFailsWith<RuntimeException> {
                    SensitiveSettingsSerializer.readFrom(bufferWith(byteArrayOf(1, 2, 3)))
                }

            assertEquals("Some other error", thrown.message)
            assertFalse(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }

    @Test
    fun `readFrom wraps SerializationException as CorruptionException`() =
        runTest {
            mockDecryptReturns("not valid json".toByteArray())

            assertFailsWith<CorruptionException> {
                SensitiveSettingsSerializer.readFrom(bufferWith(byteArrayOf(1, 2, 3)))
            }

            assertFalse(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }

    @Test
    fun `readFrom wraps IllegalStateException as CorruptionException`() =
        runTest {
            mockDecryptThrows(IllegalStateException("Bad state"))

            assertFailsWith<CorruptionException> {
                SensitiveSettingsSerializer.readFrom(bufferWith(byteArrayOf(1, 2, 3)))
            }

            assertFalse(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }

    @Test
    fun `readFrom wraps IllegalArgumentException as CorruptionException`() =
        runTest {
            mockDecryptThrows(IllegalArgumentException("Bad arg"))

            assertFailsWith<CorruptionException> {
                SensitiveSettingsSerializer.readFrom(bufferWith(byteArrayOf(1, 2, 3)))
            }

            assertFalse(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }

    @Test
    fun `readFrom handles BadPaddingException as keystore invalidation`() =
        runTest {
            mockDecryptThrows(javax.crypto.BadPaddingException("pad error"))

            assertFailsWith<CorruptionException> {
                SensitiveSettingsSerializer.readFrom(bufferWith(byteArrayOf(1, 2, 3)))
            }

            assertTrue(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }
}
