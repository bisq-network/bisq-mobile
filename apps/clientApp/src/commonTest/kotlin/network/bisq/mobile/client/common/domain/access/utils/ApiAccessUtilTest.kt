package network.bisq.mobile.client.common.domain.access.utils

import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ApiAccessUtilTest {
    // ========== parseAndNormalizeUrl() Tests ==========

    @Test
    fun `when localhost provided without scheme then adds http and default port 8090`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("localhost")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("localhost", result.host)
        assertEquals(8090, result.port)
    }

    @Test
    fun `when IPv4 address provided without scheme then adds http and default port 8090`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("192.168.1.100")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("192.168.1.100", result.host)
        assertEquals(8090, result.port)
    }

    @Test
    fun `when onion address provided without scheme then adds http and default port 8090`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("example.onion")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("example.onion", result.host)
        assertEquals(8090, result.port)
    }

    @Test
    fun `when regular domain provided without scheme then adds http and standard port 80`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("example.com")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("example.com", result.host)
        assertEquals(80, result.port)
    }

    @Test
    fun `when localhost provided with http scheme then preserves http scheme`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("http://localhost")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("localhost", result.host)
    }

    @Test
    fun `when localhost provided with https scheme then preserves https scheme`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("https://localhost")

        // Then
        assertNotNull(result)
        assertEquals("https", result.protocol.name)
        assertEquals("localhost", result.host)
    }

    @Test
    fun `when localhost provided with explicit port then preserves the explicit port`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("localhost:8080")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("localhost", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `when IPv4 address provided with explicit port then preserves the explicit port`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("192.168.1.100:9090")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("192.168.1.100", result.host)
        assertEquals(9090, result.port)
    }

    @Test
    fun `when onion URL provided with explicit port then preserves the explicit port`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("http://example.onion:8080")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("example.onion", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `when regular domain provided with explicit port then preserves the explicit port`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("http://example.com:8080")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("example.com", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `when regular domain provided with http scheme then uses standard port 80`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("http://example.com")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("example.com", result.host)
        assertEquals(80, result.port)
    }

    @Test
    fun `when localhost provided with whitespace then trims whitespace and parses correctly`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("  localhost  ")

        // Then
        assertNotNull(result)
        assertEquals("localhost", result.host)
        assertEquals(8090, result.port)
    }

    @Test
    fun `when empty string provided then returns null`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("")

        // Then
        assertNull(result)
    }

    @Test
    fun `when invalid URL format provided then returns null`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("not a valid url @#$%")

        // Then
        assertNull(result)
    }

    @Test
    fun `when onion address provided with different casing then normalizes to port 8090`() {
        // Given
        val lowerCase = "example.onion"
        val upperCase = "example.ONION"
        val mixedCase = "example.Onion"

        // When
        val resultLower = ApiAccessUtil.parseAndNormalizeUrl(lowerCase)
        val resultUpper = ApiAccessUtil.parseAndNormalizeUrl(upperCase)
        val resultMixed = ApiAccessUtil.parseAndNormalizeUrl(mixedCase)

        // Then
        assertNotNull(resultLower)
        assertNotNull(resultUpper)
        assertNotNull(resultMixed)
        assertEquals(8090, resultLower.port)
        assertEquals(8090, resultUpper.port)
        assertEquals(8090, resultMixed.port)
    }

    @Test
    fun `when localhost provided with port and path then parses host and port correctly`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("localhost:8080/api/v1")

        // Then
        assertNotNull(result)
        assertEquals("http", result.protocol.name)
        assertEquals("localhost", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `when onion URL provided with https scheme and port then preserves scheme and port`() {
        // When
        val result = ApiAccessUtil.parseAndNormalizeUrl("https://example.onion:443")

        // Then
        assertNotNull(result)
        assertEquals("https", result.protocol.name)
        assertEquals("example.onion", result.host)
        assertEquals(443, result.port)
    }

    // ========== getProxyOptionFromRestUrl() Tests ==========

    @Test
    fun `when onion URL provided then returns INTERNAL_TOR`() {
        // When
        val result = ApiAccessUtil.getProxyOptionFromRestUrl("http://example.onion:8090")

        // Then
        assertEquals(BisqProxyOption.INTERNAL_TOR, result)
    }

    @Test
    fun `when onion URL without explicit port provided then returns INTERNAL_TOR`() {
        // When
        val result = ApiAccessUtil.getProxyOptionFromRestUrl("example.onion")

        // Then
        assertEquals(BisqProxyOption.INTERNAL_TOR, result)
    }

    @Test
    fun `when localhost URL provided then returns NONE`() {
        // When
        val result = ApiAccessUtil.getProxyOptionFromRestUrl("http://localhost:8090")

        // Then
        assertEquals(BisqProxyOption.NONE, result)
    }

    @Test
    fun `when regular domain URL provided then returns NONE`() {
        // When
        val result = ApiAccessUtil.getProxyOptionFromRestUrl("http://example.com:8090")

        // Then
        assertEquals(BisqProxyOption.NONE, result)
    }

    @Test
    fun `when invalid URL provided then returns NONE`() {
        // When
        val result = ApiAccessUtil.getProxyOptionFromRestUrl("not a valid url")

        // Then
        assertEquals(BisqProxyOption.NONE, result)
    }
}
