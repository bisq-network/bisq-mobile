package network.bisq.mobile.client.common.domain.access.security

import android.util.Base64
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import network.bisq.mobile.client.common.domain.access.pairing.qr.ANDROID_LOCALHOST
import network.bisq.mobile.client.common.domain.access.pairing.qr.LOOPBACK
import java.security.MessageDigest
import java.security.cert.X509Certificate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TlsTrustManagerTest {
    private val certBytes = "fake-cert-bytes".toByteArray()
    private val certHash =
        MessageDigest
            .getInstance("SHA-256")
            .digest(certBytes)
    private val validFingerprint =
        java.util.Base64
            .getEncoder()
            .encodeToString(certHash)

    @BeforeTest
    fun setup() {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64
                .getDecoder()
                .decode(firstArg<String>())
        }
    }

    @AfterTest
    fun teardown() {
        unmockkStatic(Base64::class)
    }

    private fun certWithSanAndBytes(
        sanHost: String,
        sanType: Int = 7,
        encoded: ByteArray = certBytes,
    ): X509Certificate =
        mockk {
            every { subjectAlternativeNames } returns listOf(listOf(sanType, sanHost))
            every { getEncoded() } returns encoded
        }

    @Test
    fun `accepts valid cert with matching fingerprint and SAN IP`() {
        val cert = certWithSanAndBytes(LOOPBACK)
        val trustManager = TlsTrustManager(LOOPBACK, validFingerprint)

        // Should not throw
        trustManager.checkServerTrusted(arrayOf(cert), "RSA")
    }

    @Test
    fun `accepts valid cert with matching fingerprint and SAN DNS`() {
        val cert = certWithSanAndBytes("example.com", sanType = 2)
        val trustManager = TlsTrustManager("example.com", validFingerprint)

        trustManager.checkServerTrusted(arrayOf(cert), "RSA")
    }

    @Test
    fun `rejects cert with wrong fingerprint`() {
        val cert = certWithSanAndBytes(LOOPBACK, encoded = "different-cert".toByteArray())
        val trustManager = TlsTrustManager(LOOPBACK, validFingerprint)

        assertFailsWith<SecurityException> {
            trustManager.checkServerTrusted(arrayOf(cert), "RSA")
        }
    }

    @Test
    fun `rejects cert with non-matching SAN`() {
        val cert = certWithSanAndBytes("192.168.1.1")
        val trustManager = TlsTrustManager("10.0.0.1", validFingerprint)

        assertFailsWith<SecurityException> {
            trustManager.checkServerTrusted(arrayOf(cert), "RSA")
        }
    }

    @Test
    fun `throws on null certificate chain`() {
        val trustManager = TlsTrustManager(LOOPBACK, validFingerprint)

        assertFailsWith<IllegalArgumentException> {
            trustManager.checkServerTrusted(null, "RSA")
        }
    }

    @Test
    fun `throws on empty certificate chain`() {
        val trustManager = TlsTrustManager(LOOPBACK, validFingerprint)

        assertFailsWith<IllegalArgumentException> {
            trustManager.checkServerTrusted(emptyArray(), "RSA")
        }
    }

    @Test
    fun `maps ANDROID_LOCALHOST to LOOPBACK for SAN check`() {
        // Server cert has SAN for 127.0.0.1, but client uses 10.0.2.2
        val cert = certWithSanAndBytes(LOOPBACK)
        val trustManager = TlsTrustManager(ANDROID_LOCALHOST, validFingerprint)

        // Should succeed because ANDROID_LOCALHOST gets mapped to LOOPBACK
        trustManager.checkServerTrusted(arrayOf(cert), "RSA")
    }

    @Test
    fun `falls back to localhost when LOOPBACK SAN fails`() {
        // Cert has "localhost" as DNS SAN, not 127.0.0.1
        val cert = certWithSanAndBytes("localhost", sanType = 2, encoded = certBytes)
        val trustManager = TlsTrustManager(ANDROID_LOCALHOST, validFingerprint)

        // Should succeed because after LOOPBACK fails, it tries "localhost"
        trustManager.checkServerTrusted(arrayOf(cert), "RSA")
    }

    @Test
    fun `accepts onion host with valid fingerprint regardless of SAN`() {
        // Cert has SAN for 127.0.0.1, not for the .onion address.
        // SAN check should be skipped for .onion hosts.
        val cert = certWithSanAndBytes(LOOPBACK)
        val onionHost = "m4x6kz3javsc3xyaurp7v3za4yuwaygezmvsrrojfgzeutw2g2fm5rad.onion"
        val trustManager = TlsTrustManager(onionHost, validFingerprint)

        // Should succeed — SAN mismatch is ignored for .onion, fingerprint matches
        trustManager.checkServerTrusted(arrayOf(cert), "RSA")
    }

    @Test
    fun `rejects onion host with wrong fingerprint`() {
        val cert = certWithSanAndBytes(LOOPBACK, encoded = "different-cert".toByteArray())
        val onionHost = "m4x6kz3javsc3xyaurp7v3za4yuwaygezmvsrrojfgzeutw2g2fm5rad.onion"
        val trustManager = TlsTrustManager(onionHost, validFingerprint)

        // Fingerprint mismatch should still be rejected even for .onion
        assertFailsWith<SecurityException> {
            trustManager.checkServerTrusted(arrayOf(cert), "RSA")
        }
    }

    @Test
    fun `checkClientTrusted throws UnsupportedOperationException`() {
        val trustManager = TlsTrustManager(LOOPBACK, validFingerprint)

        assertFailsWith<UnsupportedOperationException> {
            trustManager.checkClientTrusted(null, "RSA")
        }
    }

    @Test
    fun `getAcceptedIssuers returns empty array`() {
        val trustManager = TlsTrustManager(LOOPBACK, validFingerprint)

        assertTrue(trustManager.acceptedIssuers.isEmpty())
    }

    @Test
    fun `checkServerTrusted catches exceptions during certificate processing`() {
        val cert = mockk<X509Certificate>()
        every { cert.encoded } throws Exception("Test exception during encoding")
        val trustManager = TlsTrustManager(LOOPBACK, validFingerprint)

        assertFailsWith<SecurityException> {
            trustManager.checkServerTrusted(arrayOf(cert), "RSA")
        }.also {
            assertTrue("TLS trust check failed" in (it.message ?: ""))
        }
    }
}
