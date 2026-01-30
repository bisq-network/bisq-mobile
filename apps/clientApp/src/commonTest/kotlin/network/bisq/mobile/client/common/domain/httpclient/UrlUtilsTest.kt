package network.bisq.mobile.client.common.domain.httpclient

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlUtilsTest {
    private val defaultPort = 8090

    @Test
    fun `adds http scheme when missing and appends default port`() {
        val input = "localhost"
        val expected = "http://localhost:$defaultPort"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `strips invalid scheme like localhost and forces http`() {
        val input = "localhost://localhost"
        val expected = "http://localhost:$defaultPort"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `preserves https and adds default port if missing`() {
        val input = "https://node.example"
        val expected = "https://node.example:$defaultPort"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `keeps explicit port and prepends http if scheme missing`() {
        val input = "192.168.1.5:8080"
        val expected = "http://192.168.1.5:8080"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `preserves path component when adding scheme`() {
        val input = "localhost:8081/api/v1"
        val expected = "http://localhost:8081/api/v1"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `preserves explicit port for onion URLs with http scheme`() {
        val input = "http://uhw224s7asl3m43p7kzdk2yoflweswq54zmnzvnsnrxucnlamdltx6id.onion:80"
        val expected = "http://uhw224s7asl3m43p7kzdk2yoflweswq54zmnzvnsnrxucnlamdltx6id.onion:80"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `preserves explicit non-default port for http URLs`() {
        val input = "http://example.com:8080"
        val expected = "http://example.com:8080"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `handles whitespace in input`() {
        val input = "  localhost  "
        val expected = "http://localhost:$defaultPort"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `handles wss scheme by replacing with http`() {
        // wss:// is not recognized as valid http/https and gets replaced
        val input = "wss://example.com"
        val expected = "http://example.com:$defaultPort"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `handles ws scheme by replacing with http`() {
        // ws:// is not recognized as valid http/https and gets replaced
        val input = "ws://example.com"
        val expected = "http://example.com:$defaultPort"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `handles IP address without port`() {
        val input = "192.168.1.100"
        val expected = "http://192.168.1.100:$defaultPort"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `handles custom scheme replacement`() {
        val input = "ftp://example.com"
        val expected = "http://example.com:$defaultPort"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `handles path without port`() {
        val input = "example.com/api/v1"
        val expected = "http://example.com:$defaultPort/api/v1"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `handles https with path and no port`() {
        val input = "https://example.com/api"
        val expected = "https://example.com:$defaultPort/api"
        assertEquals(expected, sanitizeBaseUrl(input, defaultPort))
    }

    @Test
    fun `handles different default port`() {
        val input = "localhost"
        val expected = "http://localhost:9090"
        assertEquals(expected, sanitizeBaseUrl(input, 9090))
    }
}
