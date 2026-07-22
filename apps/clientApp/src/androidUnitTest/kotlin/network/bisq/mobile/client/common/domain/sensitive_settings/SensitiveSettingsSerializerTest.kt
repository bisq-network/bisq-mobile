package network.bisq.mobile.client.common.domain.sensitive_settings

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.datastore.core.CorruptionException
import io.ktor.utils.io.core.toByteArray
import io.mockk.coEvery
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.data.crypto.decrypt
import network.bisq.mobile.data.crypto.encrypt
import network.bisq.mobile.data.datastore.dataStoreJson
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Keystore invalidation covers every type in [SensitiveSettingsSerializer]'s
 * `isKeystoreInvalidation()` set: direct [AEADBadTagException], direct
 * [KeyPermanentlyInvalidatedException], and nested [BadPaddingException].
 * Other unexpected [Exception]s from decrypt are rethrown unchanged.
 */
class SensitiveSettingsSerializerTest {
    @Before
    fun setUp() {
        SensitiveSettingsSerializer.resetKeystoreInvalidatedForTest()
        mockkStatic(::encrypt, ::decrypt)
        coEvery { encrypt(any()) } answers {
            val plaintext = firstArg<ByteArray>()
            // 12-byte IV prefix + test marker + plaintext (minimum total size matches LocalEncryption).
            ByteArray(MOCK_IV_LENGTH_BYTES) + MOCK_PAYLOAD_MARKER + plaintext
        }
        coEvery { decrypt(any()) } answers {
            val payload = firstArg<ByteArray>()
            if (payload.size < MOCK_MIN_ENCRYPTED_SIZE_BYTES) {
                throw IllegalStateException("Encrypted payload too short to contain IV and authentication tag")
            }
            val withoutIv = payload.copyOfRange(MOCK_IV_LENGTH_BYTES, payload.size)
            require(withoutIv.size >= MOCK_PAYLOAD_MARKER.size)
            require(withoutIv.copyOfRange(0, MOCK_PAYLOAD_MARKER.size).contentEquals(MOCK_PAYLOAD_MARKER))
            withoutIv.copyOfRange(MOCK_PAYLOAD_MARKER.size, withoutIv.size)
        }
    }

    @After
    fun tearDown() {
        SensitiveSettingsSerializer.resetKeystoreInvalidatedForTest()
        unmockkStatic(::encrypt, ::decrypt)
    }

    @Test
    fun `defaultValue returns empty SensitiveSettings`() {
        assertEquals(SensitiveSettings(), SensitiveSettingsSerializer.defaultValue)
    }

    @Test
    fun `readFrom returns default when source is exhausted`() =
        runTest {
            val result = SensitiveSettingsSerializer.readFrom(Buffer())

            assertEquals(SensitiveSettings(), result)
        }

    @Test
    fun `readFrom deserializes valid encrypted JSON`() =
        runTest {
            val expected = sampleSettings()
            val json = dataStoreJson.encodeToString(SensitiveSettings.serializer(), expected)
            val encrypted = encrypt(json.toByteArray())

            val result = SensitiveSettingsSerializer.readFrom(Buffer().write(encrypted))

            assertEquals(expected, result)
        }

    @Test
    fun `readFrom wraps SerializationException in CorruptionException`() =
        runTest {
            val encrypted = encrypt("not valid json".toByteArray())
            val exception =
                assertFailsWith<CorruptionException> {
                    SensitiveSettingsSerializer.readFrom(Buffer().write(encrypted))
                }

            assertEquals("Cannot deserialize SensitiveSettings", exception.message)
            assertIs<SerializationException>(exception.cause)
        }

    @Test
    fun `writeTo round trips SensitiveSettings`() =
        runTest {
            val original = sampleSettings()
            val buffer = Buffer()

            SensitiveSettingsSerializer.writeTo(original, buffer)
            val restored = SensitiveSettingsSerializer.readFrom(buffer)

            assertEquals(original, restored)
        }

    @Test
    fun `readFrom wraps short payload in CorruptionException`() =
        runTest {
            val exception =
                assertFailsWith<CorruptionException> {
                    SensitiveSettingsSerializer.readFrom(
                        Buffer().write(ByteArray(MOCK_MIN_ENCRYPTED_SIZE_BYTES - 1)),
                    )
                }

            assertEquals("Cannot decrypt SensitiveSettings", exception.message)
            assertIs<IllegalStateException>(exception.cause)
        }

    @Test
    fun `readFrom wraps IllegalArgumentException in CorruptionException`() =
        runTest {
            val exception =
                assertFailsWith<CorruptionException> {
                    SensitiveSettingsSerializer.readFrom(
                        Buffer().write(ByteArray(MOCK_MIN_ENCRYPTED_SIZE_BYTES)),
                    )
                }

            assertEquals("Cannot read SensitiveSettings", exception.message)
            assertIs<IllegalArgumentException>(exception.cause)
        }

    @Test
    fun `readFrom rethrows unexpected decrypt failures`() =
        runTest {
            coEvery { decrypt(any()) } throws RuntimeException("unexpected decrypt failure")

            val exception =
                assertFailsWith<RuntimeException> {
                    SensitiveSettingsSerializer.readFrom(
                        Buffer().write(ByteArray(MOCK_MIN_ENCRYPTED_SIZE_BYTES)),
                    )
                }

            assertEquals("unexpected decrypt failure", exception.message)
            assertFalse(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }

    @Test
    fun `keystoreInvalidated remains false after successful read`() =
        runTest {
            val original = sampleSettings()
            val buffer = Buffer()

            SensitiveSettingsSerializer.writeTo(original, buffer)
            SensitiveSettingsSerializer.readFrom(buffer)

            assertFalse(SensitiveSettingsSerializer.keystoreInvalidated.value)
        }

    @Test
    fun `readFrom sets keystoreInvalidated when decrypt throws AEADBadTagException`() =
        runTest {
            coEvery { decrypt(any()) } throws AEADBadTagException()

            val exception = readFromWithMockDecryptFailure()

            assertKeystoreInvalidatedCorruption(exception)
            assertIs<AEADBadTagException>(exception.cause)
        }

    @Test
    fun `readFrom sets keystoreInvalidated when decrypt throws KeyPermanentlyInvalidatedException`() =
        runTest {
            coEvery { decrypt(any()) } throws KeyPermanentlyInvalidatedException()

            val exception = readFromWithMockDecryptFailure()

            assertKeystoreInvalidatedCorruption(exception)
            assertIs<KeyPermanentlyInvalidatedException>(exception.cause)
        }

    @Test
    fun `readFrom sets keystoreInvalidated when decrypt throws BadPaddingException in cause chain`() =
        runTest {
            coEvery { decrypt(any()) } throws RuntimeException(BadPaddingException())

            val exception = readFromWithMockDecryptFailure()

            assertKeystoreInvalidatedCorruption(exception)
            assertIs<RuntimeException>(exception.cause)
            assertIs<BadPaddingException>(exception.cause?.cause)
        }

    private fun sampleSettings() =
        SensitiveSettings(
            clientName = "test-client",
            bisqApiUrl = "https://example.com",
            clientId = "client-id",
            selectedProxyOption = BisqProxyOption.NONE,
        )

    private companion object {
        // Matches LocalEncryption.android.kt: IV_LENGTH_BYTES (12) + GCM tag (128 bits = 16 bytes).
        const val MOCK_IV_LENGTH_BYTES = 12
        const val MOCK_GCM_TAG_LENGTH_BYTES = 16
        const val MOCK_MIN_ENCRYPTED_SIZE_BYTES = MOCK_IV_LENGTH_BYTES + MOCK_GCM_TAG_LENGTH_BYTES
        val MOCK_PAYLOAD_MARKER = "enc:".toByteArray()

        const val KEYSTORE_INVALIDATED_MESSAGE =
            "Keystore key invalidated — encrypted SensitiveSettings unrecoverable. " +
                "User must re-pair with their trusted node."
    }

    private suspend fun readFromWithMockDecryptFailure(): CorruptionException =
        assertFailsWith {
            SensitiveSettingsSerializer.readFrom(
                Buffer().write(ByteArray(MOCK_MIN_ENCRYPTED_SIZE_BYTES)),
            )
        }

    private fun assertKeystoreInvalidatedCorruption(exception: CorruptionException) {
        assertTrue(SensitiveSettingsSerializer.keystoreInvalidated.value)
        assertEquals(KEYSTORE_INVALIDATED_MESSAGE, exception.message)
    }
}
