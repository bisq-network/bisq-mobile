package network.bisq.mobile.client.common.domain.access.pairing.qr

import kotlin.test.Test
import kotlin.test.assertEquals

class PairingQrCodeFormatTest {
    @Test
    fun `VERSION is 1`() {
        assertEquals(1.toByte(), PairingQrCodeFormat.VERSION)
    }

    @Test
    fun `FLAG_TLS_FINGERPRINT is 1`() {
        assertEquals(1, PairingQrCodeFormat.FLAG_TLS_FINGERPRINT)
    }

    @Test
    fun `FLAG_TOR_CLIENT_AUTH is 2`() {
        assertEquals(2, PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH)
    }

    @Test
    fun `MAX_PAIRING_CODE_BYTES is 4096`() {
        assertEquals(4096, PairingQrCodeFormat.MAX_PAIRING_CODE_BYTES)
    }

    @Test
    fun `MAX_WS_URL_BYTES is 512`() {
        assertEquals(512, PairingQrCodeFormat.MAX_WS_URL_BYTES)
    }

    @Test
    fun `MAX_TLS_FINGERPRINT_BYTES is 128`() {
        assertEquals(128, PairingQrCodeFormat.MAX_TLS_FINGERPRINT_BYTES)
    }

    @Test
    fun `MAX_TOR_SECRET_BYTES is 256`() {
        assertEquals(256, PairingQrCodeFormat.MAX_TOR_SECRET_BYTES)
    }

    @Test
    fun `flags are distinct bit positions`() {
        // Ensure flags don't overlap
        val allFlags = PairingQrCodeFormat.FLAG_TLS_FINGERPRINT or PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH
        assertEquals(3, allFlags) // 1 | 2 = 3
    }

    @Test
    fun `flags can be combined and checked independently`() {
        val combined = PairingQrCodeFormat.FLAG_TLS_FINGERPRINT or PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH

        // Check TLS fingerprint flag
        assertEquals(
            PairingQrCodeFormat.FLAG_TLS_FINGERPRINT,
            combined and PairingQrCodeFormat.FLAG_TLS_FINGERPRINT,
        )

        // Check Tor client auth flag
        assertEquals(
            PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH,
            combined and PairingQrCodeFormat.FLAG_TOR_CLIENT_AUTH,
        )
    }
}
