package network.bisq.mobile.crypto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.ios.cfDictionaryOf
import network.bisq.mobile.ios.toByteArray
import network.bisq.mobile.ios.toNSData
import platform.Foundation.NSError
import platform.Security.*
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * iOS instrumented tests for LocalEncryption.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class LocalEncryptionIosTest {

    // Synchronous wrappers for testing
    private fun encryptSync(plaintext: ByteArray, keyAlias: String): ByteArray {
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val result = LocalEncryptionBridge.shared().encryptSyncWithData(
                data = plaintext.toNSData(),
                keyAlias = keyAlias,
                error = errorPtr.ptr
            )
            
            val error = errorPtr.value
            if (error != null) {
                throw IllegalStateException("Encryption failed: ${error.localizedDescription}")
            }
            
            return result?.toByteArray() ?: throw IllegalStateException("Encryption returned null with no error")
        }
    }

    private fun decryptSync(encrypted: ByteArray, keyAlias: String): ByteArray {
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val result = LocalEncryptionBridge.shared().decryptSyncWithData(
                data = encrypted.toNSData(),
                keyAlias = keyAlias,
                error = errorPtr.ptr
            )
            
            val error = errorPtr.value
            if (error != null) {
                throw IllegalStateException("Decryption failed: ${error.localizedDescription}")
            }
            
            return result?.toByteArray() ?: throw IllegalStateException("Decryption returned null with no error")
        }
    }

    @AfterTest
    fun cleanup() {
        // Clean up test keys after each test
        val testKeys = listOf(
            "test_key_1", "test_key_2", "test_key_3", "test_key_4", "test_key_5",
            "test_key_6", "test_key_7", "test_key_8", "test_key_9", "test_key_10",
            "test_key_11", "test_key_reuse", "test_key_null_bytes", "test_key_tamper"
        )
        
        testKeys.forEach { keyAlias ->
            deleteKey(keyAlias)
        }
    }


    private fun deleteKey(keyAlias: String) {
        memScoped {
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrAccount to "Account $keyAlias",
                kSecAttrService to "Service network.bisq.mobile"
            )

            SecItemDelete(query)
        }
    }

    @Test
    fun encryptReturnsNonEmptyByteArray() {
        val plaintext = "Hello, World!".encodeToByteArray()
        val keyAlias = "test_key_1"

        val encrypted = encryptSync(plaintext, keyAlias)

        assertTrue(encrypted.isNotEmpty())
    }

    @Test
    fun encryptReturnsDifferentDataThanPlaintext() {
        val plaintext = "Hello, World!".encodeToByteArray()
        val keyAlias = "test_key_2"

        val encrypted = encryptSync(plaintext, keyAlias)

        assertNotEquals(plaintext.toList(), encrypted.toList())
    }

    @Test
    fun decryptReturnsOriginalPlaintext() {
        val plaintext = "Hello, World!".encodeToByteArray()
        val keyAlias = "test_key_3"

        val encrypted = encryptSync(plaintext, keyAlias)
        val decrypted = decryptSync(encrypted, keyAlias)
        
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun encryptAndDecryptWithEmptyData() {
        val plaintext = ByteArray(0)
        val keyAlias = "test_key_4"

        val encrypted = encryptSync(plaintext, keyAlias)
        val decrypted = decryptSync(encrypted, keyAlias)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun encryptAndDecryptWithLargeData() {
        val plaintext = ByteArray(10000) { it.toByte() }
        val keyAlias = "test_key_5"

        val encrypted = encryptSync(plaintext, keyAlias)
        val decrypted = decryptSync(encrypted, keyAlias)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun encryptProducesDifferentCiphertextEachTimeDueToRandomIV() {
        val plaintext = "Hello, World!".encodeToByteArray()
        val keyAlias = "test_key_6"

        val encrypted1 = encryptSync(plaintext, keyAlias)
        val encrypted2 = encryptSync(plaintext, keyAlias)

        // Different IVs should produce different ciphertexts
        assertNotEquals(encrypted1.toList(), encrypted2.toList())
    }

    @Test
    fun decryptWorksWithDifferentEncryptionsOfSamePlaintext() {
        val plaintext = "Hello, World!".encodeToByteArray()
        val keyAlias = "test_key_7"

        val encrypted1 = encryptSync(plaintext, keyAlias)
        val encrypted2 = encryptSync(plaintext, keyAlias)
        
        val decrypted1 = decryptSync(encrypted1, keyAlias)
        val decrypted2 = decryptSync(encrypted2, keyAlias)

        assertContentEquals(plaintext, decrypted1)
        assertContentEquals(plaintext, decrypted2)
    }

    @Test
    fun encryptedDataIncludesIVPrefix() {
        val plaintext = "Hello, World!".encodeToByteArray()
        val keyAlias = "test_key_8"

        val encrypted = encryptSync(plaintext, keyAlias)

        // Encrypted data should be at least IV (12 bytes) + GCM tag (16 bytes)
        // Even for empty plaintext, we get IV + tag
        assertTrue(encrypted.size >= 12 + 16)
    }

    @Test
    fun encryptAndDecryptWithUnicodeData() {
        val plaintext = "Hello 👋 World 🌍 Unicode 中文".encodeToByteArray()
        val keyAlias = "test_key_9"

        val encrypted = encryptSync(plaintext, keyAlias)
        val decrypted = decryptSync(encrypted, keyAlias)

        assertContentEquals(plaintext, decrypted)
        kotlin.test.assertEquals("Hello 👋 World 🌍 Unicode 中文", decrypted.decodeToString())
    }

    @Test
    fun multipleKeysWorkIndependently() {
        val plaintext1 = "First message".encodeToByteArray()
        val plaintext2 = "Second message".encodeToByteArray()
        val keyAlias1 = "test_key_10"
        val keyAlias2 = "test_key_11"

        val encrypted1 = encryptSync(plaintext1, keyAlias1)
        val encrypted2 = encryptSync(plaintext2, keyAlias2)

        val decrypted1 = decryptSync(encrypted1, keyAlias1)
        val decrypted2 = decryptSync(encrypted2, keyAlias2)

        assertContentEquals(plaintext1, decrypted1)
        assertContentEquals(plaintext2, decrypted2)
    }

    @Test
    fun reusesSameKeyForSameAlias() {
        val plaintext1 = "First encryption".encodeToByteArray()
        val plaintext2 = "Second encryption".encodeToByteArray()
        val keyAlias = "test_key_reuse"

        // Encrypt first message
        val encrypted1 = encryptSync(plaintext1, keyAlias)
        val decrypted1 = decryptSync(encrypted1, keyAlias)
        assertContentEquals(plaintext1, decrypted1)

        // Encrypt second message with same key alias
        val encrypted2 = encryptSync(plaintext2, keyAlias)
        val decrypted2 = decryptSync(encrypted2, keyAlias)
        assertContentEquals(plaintext2, decrypted2)

        // First encrypted message should still be decryptable
        val decrypted1Again = decryptSync(encrypted1, keyAlias)
        assertContentEquals(plaintext1, decrypted1Again)
    }

    @Test
    fun handlesNullBytes() {
        val plaintext = byteArrayOf(0, 1, 2, 0, 3, 0, 0, 4)
        val keyAlias = "test_key_null_bytes"

        val encrypted = encryptSync(plaintext, keyAlias)
        val decrypted = decryptSync(encrypted, keyAlias)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun decryptFailsWhenDataIsTampered() {
        val plaintext = "Hello, World!".encodeToByteArray()
        val keyAlias = "test_key_tamper"

        val encrypted = encryptSync(plaintext, keyAlias)
        
        // Tamper with the encrypted data (modify a byte in the ciphertext portion)
        val tampered = encrypted.copyOf()
        if (tampered.size > 20) {
            tampered[20] = (tampered[20].toInt() xor 0xFF).toByte()
        }

        // Attempting to decrypt tampered data should throw an exception
        var exceptionThrown = false
        try {
            decryptSync(tampered, keyAlias)
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown, "Decryption should fail when data is tampered")
    }
}
