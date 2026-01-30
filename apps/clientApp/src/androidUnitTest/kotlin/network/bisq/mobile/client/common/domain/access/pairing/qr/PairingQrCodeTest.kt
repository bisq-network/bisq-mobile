package network.bisq.mobile.client.common.domain.access.pairing.qr

import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.access.pairing.PairingCode
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PairingQrCodeTest {
    private fun createTestPairingCode() =
        PairingCode(
            id = "test-id",
            expiresAt = Instant.fromEpochMilliseconds(1700000000000L),
            grantedPermissions = setOf(Permission.OFFERBOOK),
        )

    @Test
    fun `data class properties are accessible`() {
        val pairingCode = createTestPairingCode()

        val qrCode =
            PairingQrCode(
                version = 1,
                pairingCode = pairingCode,
                webSocketUrl = "wss://example.com:8090",
                tlsFingerprint = "abc123",
                torClientAuthSecret = "secret",
            )

        assertEquals(1.toByte(), qrCode.version)
        assertEquals(pairingCode, qrCode.pairingCode)
        assertEquals("wss://example.com:8090", qrCode.webSocketUrl)
        assertEquals("abc123", qrCode.tlsFingerprint)
        assertEquals("secret", qrCode.torClientAuthSecret)
    }

    @Test
    fun `optional fields can be null`() {
        val pairingCode = createTestPairingCode()

        val qrCode =
            PairingQrCode(
                version = 1,
                pairingCode = pairingCode,
                webSocketUrl = "wss://example.com:8090",
                tlsFingerprint = null,
                torClientAuthSecret = null,
            )

        assertNull(qrCode.tlsFingerprint)
        assertNull(qrCode.torClientAuthSecret)
    }

    @Test
    fun `data class equality works correctly`() {
        val pairingCode = createTestPairingCode()

        val qrCode1 = PairingQrCode(1, pairingCode, "wss://a.com", null, null)
        val qrCode2 = PairingQrCode(1, pairingCode, "wss://a.com", null, null)

        assertEquals(qrCode1, qrCode2)
    }

    @Test
    fun `data class copy works correctly`() {
        val pairingCode = createTestPairingCode()

        val original = PairingQrCode(1, pairingCode, "wss://a.com", null, null)
        val copied = original.copy(webSocketUrl = "wss://b.com")

        assertEquals("wss://b.com", copied.webSocketUrl)
        assertEquals(pairingCode, copied.pairingCode)
    }

    @Test
    fun `onion URL is supported`() {
        val pairingCode = createTestPairingCode()
        val onionUrl = "wss://abcdefghijklmnopqrstuvwxyz234567.onion:8090"

        val qrCode = PairingQrCode(1, pairingCode, onionUrl, null, "tor-secret")

        assertEquals(onionUrl, qrCode.webSocketUrl)
        assertEquals("tor-secret", qrCode.torClientAuthSecret)
    }
}
