package network.bisq.mobile.client.common.domain.access.security

import io.mockk.every
import io.mockk.mockk
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SanVerifierTest {
    private fun certWithSans(vararg sans: Pair<Int, String>): X509Certificate =
        mockk {
            every { subjectAlternativeNames } returns
                sans.map { (type, value) -> listOf(type, value) }
        }

    private fun certWithNoSans(): X509Certificate =
        mockk {
            every { subjectAlternativeNames } returns null
        }

    @Test
    fun `matches DNS SAN entry`() {
        val cert = certWithSans(2 to "example.com")
        assertTrue(SanVerifier.matchesHost(cert, "example.com"))
    }

    @Test
    fun `DNS match is case insensitive`() {
        val cert = certWithSans(2 to "Example.COM")
        assertTrue(SanVerifier.matchesHost(cert, "example.com"))
    }

    @Test
    fun `matches IP SAN entry`() {
        val cert = certWithSans(7 to "127.0.0.1")
        assertTrue(SanVerifier.matchesHost(cert, "127.0.0.1"))
    }

    @Test
    fun `IP match is case sensitive`() {
        val cert = certWithSans(7 to "127.0.0.1")
        assertFalse(SanVerifier.matchesHost(cert, "127.0.0.01"))
    }

    @Test
    fun `returns false when no SANs present`() {
        val cert = certWithNoSans()
        assertFalse(SanVerifier.matchesHost(cert, "example.com"))
    }

    @Test
    fun `returns false when host not in SANs`() {
        val cert = certWithSans(2 to "other.com")
        assertFalse(SanVerifier.matchesHost(cert, "example.com"))
    }

    @Test
    fun `matches among multiple SANs`() {
        val cert =
            certWithSans(
                2 to "first.com",
                2 to "second.com",
                7 to "192.168.1.1",
            )
        assertTrue(SanVerifier.matchesHost(cert, "second.com"))
        assertTrue(SanVerifier.matchesHost(cert, "192.168.1.1"))
        assertFalse(SanVerifier.matchesHost(cert, "third.com"))
    }

    @Test
    fun `matches localhost as DNS entry`() {
        val cert = certWithSans(2 to "localhost")
        assertTrue(SanVerifier.matchesHost(cert, "localhost"))
    }

    @Test
    fun `matches loopback as IP entry`() {
        val cert = certWithSans(7 to "127.0.0.1")
        assertTrue(SanVerifier.matchesHost(cert, "127.0.0.1"))
    }

    @Test
    fun `returns false on exception`() {
        val cert =
            mockk<X509Certificate> {
                every { subjectAlternativeNames } throws RuntimeException("cert error")
            }
        assertFalse(SanVerifier.matchesHost(cert, "example.com"))
    }

    @Test
    fun `ignores non-DNS non-IP SAN types`() {
        // type 1 = rfc822Name (email), should not match
        val cert = certWithSans(1 to "example.com")
        assertFalse(SanVerifier.matchesHost(cert, "example.com"))
    }
}
