package network.bisq.mobile.client.common.domain.service.push_notification

import android.util.Base64
import network.bisq.mobile.client.common.test_utils.TestApplication
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for the AES-256-GCM decryption path used by
 * `BisqFirebaseMessagingService`. We encrypt with the same wire layout
 * (`nonce(12) || ciphertext || tag(16)`) the relay produces, then assert
 * the service's decrypt method roundtrips correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class BisqFirebaseMessagingServiceTest {
    @Test
    fun `decryptAesGcm round-trips a payload encrypted with the relay wire layout`() {
        val plaintext = """{"id":"abc-123","title":"Trade update","message":"hello"}"""
        val keyBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val ciphertextWithTag = aesGcmEncrypt(plaintext.toByteArray(Charsets.UTF_8), keyBytes, nonce)
        val combinedBase64 = Base64.encodeToString(nonce + ciphertextWithTag, Base64.NO_WRAP)
        val keyBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)

        val decrypted = BisqFirebaseMessagingService.decryptAesGcm(combinedBase64, keyBase64)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `decryptAesGcm rejects payloads shorter than the nonce`() {
        val tooShort = Base64.encodeToString(ByteArray(8), Base64.NO_WRAP)
        val keyBase64 = Base64.encodeToString(ByteArray(32), Base64.NO_WRAP)

        val ex =
            assertFailsWith<IllegalArgumentException> {
                BisqFirebaseMessagingService.decryptAesGcm(tooShort, keyBase64)
            }
        assertTrue(ex.message?.contains("too short", ignoreCase = true) == true)
    }

    @Test
    fun `decryptAesGcm fails when the key does not match`() {
        val plaintext = "secret"
        val realKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val ciphertextWithTag = aesGcmEncrypt(plaintext.toByteArray(Charsets.UTF_8), realKey, nonce)
        val combinedBase64 = Base64.encodeToString(nonce + ciphertextWithTag, Base64.NO_WRAP)

        // GCM tag verification fails -> javax.crypto.AEADBadTagException
        val thrown =
            runCatching {
                BisqFirebaseMessagingService.decryptAesGcm(
                    combinedBase64,
                    Base64.encodeToString(wrongKey, Base64.NO_WRAP),
                )
            }
        assertTrue(thrown.isFailure, "decryption must fail with a mismatched key")
    }

    private fun aesGcmEncrypt(
        plaintext: ByteArray,
        keyBytes: ByteArray,
        nonce: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(128, nonce),
        )
        return cipher.doFinal(plaintext)
    }
}
