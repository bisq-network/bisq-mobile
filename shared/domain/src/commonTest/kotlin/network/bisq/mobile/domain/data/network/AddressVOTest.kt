package network.bisq.mobile.domain.data.network

import network.bisq.mobile.domain.data.replicated.common.network.AddressVO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddressVOTest {
    @Test
    fun `from should parse valid URL with schema`() {
        val result = AddressVO.from("http://example.com:8080")
        assertEquals(AddressVO("example.com", 8080), result)
    }

    @Test
    fun `from should parse valid URL without schema`() {
        val result = AddressVO.from("example.com:8080")
        assertEquals(AddressVO("example.com", 8080), result)
    }

    @Test
    fun `from should handle onion address and lowercase it`() {
        val result = AddressVO.from("http://TeSt.onion:80")
        assertEquals(AddressVO("test.onion", 80), result)
    }

    @Test
    fun `from should return null for blank URL`() {
        val result = AddressVO.from("")
        assertNull(result)
    }

    @Test
    fun `from should return null for invalid port below range`() {
        val result = AddressVO.from("example.com:0")
        assertNull(result)
    }

    @Test
    fun `from should return null for invalid port above range`() {
        val result = AddressVO.from("example.com:65536")
        assertNull(result)
    }

    @Test
    fun `from should accept min valid port`() {
        val result = AddressVO.from("example.com:1")
        assertEquals(AddressVO("example.com", 1), result)
    }

    @Test
    fun `from should accept max valid port`() {
        val result = AddressVO.from("example.com:65535")
        assertEquals(AddressVO("example.com", 65535), result)
    }

    @Test
    fun `from should return null for malformed URL`() {
        val result = AddressVO.from("not-a-url")
        assertNull(result)
    }

    @Test
    fun `from should return null for negative port`() {
        val result = AddressVO.from("example.com:-1")
        assertNull(result)
    }

    @Test
    fun `from should return null for non-numeric port`() {
        val result = AddressVO.from("example.com:abc")
        assertNull(result)
    }

    @Test
    fun `from should return null for mixed numeric-alpha port`() {
        val result = AddressVO.from("example.com:8080abc")
        assertNull(result)
    }

    @Test
    fun `from should return null for URL without host`() {
        val result = AddressVO.from("http://:8080")
        assertNull(result)
    }

    // Additional edge cases for regex-based parser
    @Test
    fun `from should handle https schema`() {
        val result = AddressVO.from("https://secure.example.com:443")
        assertEquals(AddressVO("secure.example.com", 443), result)
    }

    @Test
    fun `from should handle IP address`() {
        val result = AddressVO.from("192.168.1.1:8080")
        assertEquals(AddressVO("192.168.1.1", 8080), result)
    }

    @Test
    fun `from should handle localhost`() {
        val result = AddressVO.from("localhost:3000")
        assertEquals(AddressVO("localhost", 3000), result)
    }

    @Test
    fun `from should handle URL with path`() {
        val result = AddressVO.from("http://example.com:8080/path/to/resource")
        assertEquals(AddressVO("example.com", 8080), result)
    }

    @Test
    fun `from should trim whitespace`() {
        val result = AddressVO.from("  example.com:8080  ")
        assertEquals(AddressVO("example.com", 8080), result)
    }

    @Test
    fun `from should return null for whitespace only`() {
        val result = AddressVO.from("   ")
        assertNull(result)
    }

    @Test
    fun `from should handle subdomain`() {
        val result = AddressVO.from("api.v2.example.com:9000")
        assertEquals(AddressVO("api.v2.example.com", 9000), result)
    }

    @Test
    fun `from should return null for missing port`() {
        val result = AddressVO.from("example.com")
        assertNull(result)
    }

    @Test
    fun `from should return null for URL with schema but no port`() {
        val result = AddressVO.from("http://example.com")
        assertNull(result)
    }

    @Test
    fun `from should handle long onion address`() {
        val onion = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion"
        val result = AddressVO.from("$onion:9050")
        assertEquals(AddressVO(onion, 9050), result)
    }

    @Test
    fun `from should handle custom schema`() {
        val result = AddressVO.from("ws://websocket.example.com:8080")
        assertEquals(AddressVO("websocket.example.com", 8080), result)
    }
}
