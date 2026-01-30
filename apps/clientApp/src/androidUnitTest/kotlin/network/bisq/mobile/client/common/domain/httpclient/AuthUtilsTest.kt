package network.bisq.mobile.client.common.domain.httpclient

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthUtilsTest {
    @Test
    fun `generateAuthHash produces consistent hash for same inputs`() {
        val hash1 =
            AuthUtils.generateAuthHash(
                password = "secret",
                nonce = "abc123",
                timestamp = "1700000000",
                method = "GET",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        val hash2 =
            AuthUtils.generateAuthHash(
                password = "secret",
                nonce = "abc123",
                timestamp = "1700000000",
                method = "GET",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        assertEquals(hash1, hash2)
    }

    @Test
    fun `generateAuthHash produces different hash for different passwords`() {
        val hash1 =
            AuthUtils.generateAuthHash(
                password = "secret1",
                nonce = "abc123",
                timestamp = "1700000000",
                method = "GET",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        val hash2 =
            AuthUtils.generateAuthHash(
                password = "secret2",
                nonce = "abc123",
                timestamp = "1700000000",
                method = "GET",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `generateAuthHash produces different hash for different nonces`() {
        val hash1 =
            AuthUtils.generateAuthHash(
                password = "secret",
                nonce = "nonce1",
                timestamp = "1700000000",
                method = "GET",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        val hash2 =
            AuthUtils.generateAuthHash(
                password = "secret",
                nonce = "nonce2",
                timestamp = "1700000000",
                method = "GET",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `generateAuthHash normalizes method to uppercase`() {
        val hashLower =
            AuthUtils.generateAuthHash(
                password = "secret",
                nonce = "abc123",
                timestamp = "1700000000",
                method = "get",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        val hashUpper =
            AuthUtils.generateAuthHash(
                password = "secret",
                nonce = "abc123",
                timestamp = "1700000000",
                method = "GET",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        assertEquals(hashLower, hashUpper)
    }

    @Test
    fun `generateAuthHash includes body hash when provided`() {
        val hashWithBody =
            AuthUtils.generateAuthHash(
                password = "secret",
                nonce = "abc123",
                timestamp = "1700000000",
                method = "POST",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = "bodyhash123",
            )

        val hashWithoutBody =
            AuthUtils.generateAuthHash(
                password = "secret",
                nonce = "abc123",
                timestamp = "1700000000",
                method = "POST",
                normalizedPath = "/api/v1/test",
                bodySha256Hex = null,
            )

        assertNotEquals(hashWithBody, hashWithoutBody)
    }

    @Test
    fun `getNormalizedPathAndQuery returns path for simple URL`() {
        val url = Url("http://example.com/api/v1/test")
        val result = AuthUtils.getNormalizedPathAndQuery(url)
        assertEquals("/api/v1/test", result)
    }

    @Test
    fun `getNormalizedPathAndQuery includes query string`() {
        val url = Url("http://example.com/api/v1/test?param=value")
        val result = AuthUtils.getNormalizedPathAndQuery(url)
        assertEquals("/api/v1/test?param=value", result)
    }

    @Test
    fun `getNormalizedPathAndQuery trims trailing slash`() {
        val url = Url("http://example.com/api/v1/test/")
        val result = AuthUtils.getNormalizedPathAndQuery(url)
        assertEquals("/api/v1/test", result)
    }

    @Test
    fun `getNormalizedPathAndQuery preserves root path`() {
        val url = Url("http://example.com/")
        val result = AuthUtils.getNormalizedPathAndQuery(url)
        assertEquals("/", result)
    }

    @Test
    fun `generateNonce produces hex string`() {
        val nonce = AuthUtils.generateNonce()
        assertTrue(nonce.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `generateNonce produces different values each time`() {
        val nonce1 = AuthUtils.generateNonce()
        val nonce2 = AuthUtils.generateNonce()
        assertNotEquals(nonce1, nonce2)
    }

    @Test
    fun `generateNonce with custom byte count produces correct length`() {
        val nonce = AuthUtils.generateNonce(16)
        // 16 bytes = 32 hex characters
        assertEquals(32, nonce.length)
    }
}
