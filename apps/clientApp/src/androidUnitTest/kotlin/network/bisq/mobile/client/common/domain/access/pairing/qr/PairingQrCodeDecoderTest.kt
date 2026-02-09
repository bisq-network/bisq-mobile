package network.bisq.mobile.client.common.domain.access.pairing.qr

import io.mockk.every
import io.mockk.mockk
import network.bisq.mobile.client.common.domain.access.pairing.PairingCode
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.utils.BinaryEncodingUtils
import network.bisq.mobile.client.common.domain.utils.BinaryWriter
import network.bisq.mobile.domain.data.EnvironmentController
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalEncodingApi::class)
class PairingQrCodeDecoderTest {
    private val environmentController =
        mockk<EnvironmentController> {
            every { isSimulator() } returns true
        }
    private val decoder = PairingQrCodeDecoder(environmentController)

    private fun encodePairingCodeBytes(
        version: Byte = PairingCode.VERSION,
        id: String = "test-id",
        expiresAtMillis: Long = 1700000000000L,
        permissions: Set<Permission> = setOf(Permission.OFFERBOOK),
    ): ByteArray {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeByte(writer, version)
        BinaryEncodingUtils.writeString(writer, id)
        BinaryEncodingUtils.writeLong(writer, expiresAtMillis)
        BinaryEncodingUtils.writeInt(writer, permissions.size)
        permissions.forEach { permission ->
            BinaryEncodingUtils.writeInt(writer, permission.id)
        }
        return writer.toByteArray()
    }

    private fun encodeQrCode(
        version: Byte = PairingQrCodeFormat.VERSION,
        pairingCodeBytes: ByteArray = encodePairingCodeBytes(),
        webSocketUrl: String = "wss://example.com:8090",
        flags: Byte = 0,
        tlsFingerprint: String? = null,
        torClientAuthSecret: String? = null,
    ): ByteArray {
        val writer = BinaryWriter()
        BinaryEncodingUtils.writeByte(writer, version)
        BinaryEncodingUtils.writeBytes(writer, pairingCodeBytes, PairingQrCodeFormat.MAX_PAIRING_CODE_BYTES)
        BinaryEncodingUtils.writeString(writer, webSocketUrl, PairingQrCodeFormat.MAX_WS_URL_BYTES)
        BinaryEncodingUtils.writeByte(writer, flags)

        if (tlsFingerprint != null) {
            BinaryEncodingUtils.writeString(writer, tlsFingerprint, PairingQrCodeFormat.MAX_TLS_FINGERPRINT_BYTES)
        }
        if (torClientAuthSecret != null) {
            BinaryEncodingUtils.writeString(writer, torClientAuthSecret, PairingQrCodeFormat.MAX_TOR_SECRET_BYTES)
        }

        return writer.toByteArray()
    }

    @Test
    fun `decode bytes returns correct PairingQrCode`() {
        val bytes =
            encodeQrCode(
                webSocketUrl = "wss://test.example.com:8090",
            )

        val result = decoder.decode(bytes)

        assertEquals(PairingQrCodeFormat.VERSION, result.version)
        assertEquals("wss://test.example.com:8090", result.webSocketUrl)
        assertNotNull(result.pairingCode)
        assertNull(result.tlsFingerprint)
        assertNull(result.torClientAuthSecret)
    }

    @Test
    fun `decode base64 returns correct PairingQrCode`() {
        val bytes = encodeQrCode(webSocketUrl = "wss://base64.test:8090")
        val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)

        val result = decoder.decode(base64)

        assertEquals("wss://base64.test:8090", result.webSocketUrl)
    }

    @Test
    fun `decode with TLS fingerprint flag returns fingerprint`() {
        val bytes =
            encodeQrCode(
                flags = PairingQrCodeFormat.FLAG_TLS_FINGERPRINT.toByte(),
                tlsFingerprint = "abc123fingerprint",
            )

        val result = decoder.decode(bytes)

        assertEquals("abc123fingerprint", result.tlsFingerprint)
        assertNull(result.torClientAuthSecret)
    }

    @Test
    fun `decode with Tor client auth flag returns secret`() {
        val bytes =
            encodeQrCode(
                flags = PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH.toByte(),
                torClientAuthSecret = "tor-secret-key",
            )

        val result = decoder.decode(bytes)

        assertNull(result.tlsFingerprint)
        assertEquals("tor-secret-key", result.torClientAuthSecret)
    }

    @Test
    fun `decode with both flags returns both values`() {
        val combinedFlags = (PairingQrCodeFormat.FLAG_TLS_FINGERPRINT or PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH).toByte()
        val bytes =
            encodeQrCode(
                flags = combinedFlags,
                tlsFingerprint = "fingerprint",
                torClientAuthSecret = "tor-secret",
            )

        val result = decoder.decode(bytes)

        assertEquals("fingerprint", result.tlsFingerprint)
        assertEquals("tor-secret", result.torClientAuthSecret)
    }

    @Test
    fun `decode throws for unsupported version`() {
        val bytes = encodeQrCode(version = 99)

        assertFailsWith<IllegalArgumentException> {
            decoder.decode(bytes)
        }
    }

    @Test
    fun `decode with onion URL works`() {
        val onionUrl = "wss://abcdefghijklmnopqrstuvwxyz234567.onion:8090"
        val bytes = encodeQrCode(webSocketUrl = onionUrl)

        val result = decoder.decode(bytes)

        assertEquals(onionUrl, result.webSocketUrl)
    }

    @Test
    fun `decode preserves pairing code permissions`() {
        val pairingCodeBytes =
            encodePairingCodeBytes(
                permissions = setOf(Permission.OFFERBOOK, Permission.TRADES, Permission.SETTINGS),
            )
        val bytes = encodeQrCode(pairingCodeBytes = pairingCodeBytes)

        val result = decoder.decode(bytes)

        assertEquals(3, result.pairingCode.grantedPermissions.size)
    }

    @Test
    fun `decode with localhost URL works`() {
        val bytes = encodeQrCode(webSocketUrl = "ws://localhost:8090")

        val result = decoder.decode(bytes)

        assertEquals("ws://$ANDROID_LOCALHOST:8090", result.webSocketUrl)
    }

    @Test
    fun `decode with IP address URL works`() {
        val bytes = encodeQrCode(webSocketUrl = "wss://192.168.1.100:8090")

        val result = decoder.decode(bytes)

        assertEquals("wss://192.168.1.100:8090", result.webSocketUrl)
    }

    @Test
    fun `decode preserves pairing code id`() {
        val pairingCodeBytes = encodePairingCodeBytes(id = "unique-pairing-id-123")
        val bytes = encodeQrCode(pairingCodeBytes = pairingCodeBytes)

        val result = decoder.decode(bytes)

        assertEquals("unique-pairing-id-123", result.pairingCode.id)
    }
}
