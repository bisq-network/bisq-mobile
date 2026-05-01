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

    // ----- NotificationCategory tests -----

    @Test
    fun `fromPayload prefers the explicit category id when present`() {
        val payload =
            BisqFirebaseMessagingService.NotificationPayload(
                id = "1",
                title = "Random title that would otherwise classify as GENERAL",
                message = "msg",
                category = "trade_update",
            )

        val category = BisqFirebaseMessagingService.NotificationCategory.fromPayload(payload)

        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
            category,
        )
    }

    @Test
    fun `fromPayload falls back to title parsing when category is null`() {
        val payload =
            BisqFirebaseMessagingService.NotificationPayload(
                id = "1",
                title = "New chat message arrived",
                message = "msg",
                category = null,
            )

        val category = BisqFirebaseMessagingService.NotificationCategory.fromPayload(payload)

        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
            category,
        )
    }

    @Test
    fun `fromPayload returns GENERAL when explicit category id is unknown to this client`() {
        // Contract: an explicit `category` from the trusted node is the stable
        // wire signal. If we don't recognize it (e.g., a newer bisq2 introduced
        // `dispute_alert`), we return GENERAL rather than running the title
        // heuristic — the trusted node already told us this is a specific
        // category, we just don't know which one yet, so showing the generic
        // banner is more honest than guessing from the title.
        val payload =
            BisqFirebaseMessagingService.NotificationPayload(
                id = "1",
                title = "Trade update", // would have matched TRADE_UPDATE under the old behavior
                message = "msg",
                category = "made-up-category-from-some-future-bisq2",
            )

        val category = BisqFirebaseMessagingService.NotificationCategory.fromPayload(payload)

        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.GENERAL,
            category,
        )
    }

    @Test
    fun `fromTitle classifies trade and payment and btc keywords as TRADE_UPDATE`() {
        listOf("Trade started", "Payment received", "BTC confirmed").forEach { title ->
            assertEquals(
                BisqFirebaseMessagingService.NotificationCategory.TRADE_UPDATE,
                BisqFirebaseMessagingService.NotificationCategory.fromTitle(title),
                "unexpected category for title: $title",
            )
        }
    }

    @Test
    fun `fromTitle classifies message and chat keywords as CHAT_MESSAGE`() {
        listOf("New message", "Chat update", "MESSAGE waiting").forEach { title ->
            assertEquals(
                BisqFirebaseMessagingService.NotificationCategory.CHAT_MESSAGE,
                BisqFirebaseMessagingService.NotificationCategory.fromTitle(title),
                "unexpected category for title: $title",
            )
        }
    }

    @Test
    fun `fromTitle classifies offer keyword as OFFER_UPDATE`() {
        assertEquals(
            BisqFirebaseMessagingService.NotificationCategory.OFFER_UPDATE,
            BisqFirebaseMessagingService.NotificationCategory.fromTitle("New offer matched"),
        )
    }

    @Test
    fun `fromTitle returns GENERAL for unmatched titles`() {
        listOf("Something else", "Hello world", "").forEach { title ->
            assertEquals(
                BisqFirebaseMessagingService.NotificationCategory.GENERAL,
                BisqFirebaseMessagingService.NotificationCategory.fromTitle(title),
                "unexpected category for title: $title",
            )
        }
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
