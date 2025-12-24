package network.bisq.mobile.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Android instrumented tests for LocalEncryption.
 */
@RunWith(AndroidJUnit4::class)
class LocalEncryptionInstrumentedTest {
    @After
    fun cleanup() {
        // Clean up test keys after each test
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        listOf(
            "test_key_1",
            "test_key_2",
            "test_key_3",
            "test_key_4",
            "test_key_5",
            "test_key_6",
            "test_key_7",
            "test_key_8",
            "test_key_9",
            "test_key_10",
            "test_key_11",
            "test_key_reuse",
            "test_key_null_bytes",
            "test_key_tamper",
        ).forEach { alias ->
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        }
    }

    @Test
    fun encryptReturnsNonEmptyByteArray() =
        runTest {
            val plaintext = "Hello, World!".toByteArray()
            val keyAlias = "test_key_1"

            val encrypted = encrypt(plaintext, keyAlias)

            assertTrue(encrypted.isNotEmpty())
        }

    @Test
    fun encryptReturnsDifferentDataThanPlaintext() =
        runTest {
            val plaintext = "Hello, World!".toByteArray()
            val keyAlias = "test_key_2"

            val encrypted = encrypt(plaintext, keyAlias)

            assertNotEquals(plaintext.toList(), encrypted.toList())
        }

    @Test
    fun decryptReturnsOriginalPlaintext() =
        runTest {
            val plaintext = "Hello, World!".toByteArray()
            val keyAlias = "test_key_3"

            val encrypted = encrypt(plaintext, keyAlias)
            val decrypted = decrypt(encrypted, keyAlias)

            assertContentEquals(plaintext, decrypted)
        }

    @Test
    fun encryptAndDecryptWithEmptyData() =
        runTest {
            val plaintext = ByteArray(0)
            val keyAlias = "test_key_4"

            val encrypted = encrypt(plaintext, keyAlias)
            val decrypted = decrypt(encrypted, keyAlias)

            assertContentEquals(plaintext, decrypted)
        }

    @Test
    fun encryptAndDecryptWithLargeData() =
        runTest {
            val plaintext = ByteArray(10000) { it.toByte() }
            val keyAlias = "test_key_5"

            val encrypted = encrypt(plaintext, keyAlias)
            val decrypted = decrypt(encrypted, keyAlias)

            assertContentEquals(plaintext, decrypted)
        }

    @Test
    fun encryptProducesDifferentCiphertextEachTimeDueToRandomIV() =
        runTest {
            val plaintext = "Hello, World!".toByteArray()
            val keyAlias = "test_key_6"

            val encrypted1 = encrypt(plaintext, keyAlias)
            val encrypted2 = encrypt(plaintext, keyAlias)

            // Different IVs should produce different ciphertexts
            assertNotEquals(encrypted1.toList(), encrypted2.toList())
        }

    @Test
    fun decryptWorksWithDifferentEncryptionsOfSamePlaintext() =
        runTest {
            val plaintext = "Hello, World!".toByteArray()
            val keyAlias = "test_key_7"

            val encrypted1 = encrypt(plaintext, keyAlias)
            val encrypted2 = encrypt(plaintext, keyAlias)

            val decrypted1 = decrypt(encrypted1, keyAlias)
            val decrypted2 = decrypt(encrypted2, keyAlias)

            assertContentEquals(plaintext, decrypted1)
            assertContentEquals(plaintext, decrypted2)
        }

    @Test
    fun encryptedDataIncludesIVPrefix() =
        runTest {
            val plaintext = "Hello, World!".toByteArray()
            val keyAlias = "test_key_8"

            val encrypted = encrypt(plaintext, keyAlias)

            // Encrypted data should be at least IV (12 bytes) + GCM tag (16 bytes)
            // Even for empty plaintext, we get IV + tag
            assertTrue(encrypted.size >= 12 + 16)
        }

    @Test
    fun encryptAndDecryptWithUnicodeData() =
        runTest {
            val plaintext = "Hello 👋 World 🌍 Unicode 中文".toByteArray(Charsets.UTF_8)
            val keyAlias = "test_key_9"

            val encrypted = encrypt(plaintext, keyAlias)
            val decrypted = decrypt(encrypted, keyAlias)

            assertContentEquals(plaintext, decrypted)
            kotlin.test.assertEquals("Hello 👋 World 🌍 Unicode 中文", decrypted.toString(Charsets.UTF_8))
        }

    @Test
    fun multipleKeysWorkIndependently() =
        runTest {
            val plaintext1 = "First message".toByteArray()
            val plaintext2 = "Second message".toByteArray()
            val keyAlias1 = "test_key_10"
            val keyAlias2 = "test_key_11"

            val encrypted1 = encrypt(plaintext1, keyAlias1)
            val encrypted2 = encrypt(plaintext2, keyAlias2)

            val decrypted1 = decrypt(encrypted1, keyAlias1)
            val decrypted2 = decrypt(encrypted2, keyAlias2)

            assertContentEquals(plaintext1, decrypted1)
            assertContentEquals(plaintext2, decrypted2)
        }

    @Test
    fun reusesSameKeyForSameAlias() =
        runTest {
            val plaintext1 = "First encryption".toByteArray()
            val plaintext2 = "Second encryption".toByteArray()
            val keyAlias = "test_key_reuse"

            // Encrypt first message
            val encrypted1 = encrypt(plaintext1, keyAlias)
            val decrypted1 = decrypt(encrypted1, keyAlias)
            assertContentEquals(plaintext1, decrypted1)

            // Encrypt second message with same key alias
            val encrypted2 = encrypt(plaintext2, keyAlias)
            val decrypted2 = decrypt(encrypted2, keyAlias)
            assertContentEquals(plaintext2, decrypted2)

            // First encrypted message should still be decryptable
            val decrypted1Again = decrypt(encrypted1, keyAlias)
            assertContentEquals(plaintext1, decrypted1Again)
        }

    @Test
    fun handlesNullBytes() =
        runTest {
            val plaintext = byteArrayOf(0, 1, 2, 0, 3, 0, 0, 4)
            val keyAlias = "test_key_null_bytes"

            val encrypted = encrypt(plaintext, keyAlias)
            val decrypted = decrypt(encrypted, keyAlias)

            assertContentEquals(plaintext, decrypted)
        }

    @Test
    fun decryptFailsWhenDataIsTampered() =
        runTest {
            val plaintext = "Hello, World!".toByteArray()
            val keyAlias = "test_key_tamper"

            val encrypted = encrypt(plaintext, keyAlias)

            // Tamper with the encrypted data (modify a byte in the ciphertext portion)
            val tampered = encrypted.copyOf()
            if (tampered.size > 20) {
                tampered[20] = (tampered[20].toInt() xor 0xFF).toByte()
            }

            // Attempting to decrypt tampered data should throw an exception
            var exceptionThrown = false
            try {
                decrypt(tampered, keyAlias)
            } catch (e: Exception) {
                exceptionThrown = true
            }

            assertTrue(exceptionThrown, "Decryption should fail when data is tampered")
        }
}
