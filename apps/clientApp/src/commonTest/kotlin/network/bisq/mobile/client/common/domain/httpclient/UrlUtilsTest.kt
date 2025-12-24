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
}
