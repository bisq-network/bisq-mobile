package network.bisq.mobile.domain.data.replicated.common.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AddressVOTest {
    @Test
    fun `from parses host and port`() {
        val result = AddressVO.from("example.com:8080")
        assertNotNull(result)
        assertEquals("example.com", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `from parses URL with scheme`() {
        val result = AddressVO.from("http://example.com:8080")
        assertNotNull(result)
        assertEquals("example.com", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `from parses https URL`() {
        val result = AddressVO.from("https://example.com:443")
        assertNotNull(result)
        assertEquals("example.com", result.host)
        assertEquals(443, result.port)
    }

    @Test
    fun `from parses wss URL`() {
        val result = AddressVO.from("wss://example.com:8090")
        assertNotNull(result)
        assertEquals("example.com", result.host)
        assertEquals(8090, result.port)
    }

    @Test
    fun `from returns null for blank input`() {
        assertNull(AddressVO.from(""))
        assertNull(AddressVO.from("   "))
    }

    @Test
    fun `from returns null for missing port`() {
        assertNull(AddressVO.from("example.com"))
        assertNull(AddressVO.from("http://example.com"))
    }

    @Test
    fun `from returns null for invalid port`() {
        assertNull(AddressVO.from("example.com:0"))
        assertNull(AddressVO.from("example.com:65536"))
        assertNull(AddressVO.from("example.com:abc"))
    }

    @Test
    fun `from lowercases onion addresses`() {
        // The code only lowercases if the host ends with ".onion" (lowercase)
        val result = AddressVO.from("ABCDEF.onion:8080")
        assertNotNull(result)
        assertEquals("abcdef.onion", result.host)
    }

    @Test
    fun `from preserves case for non-onion addresses`() {
        val result = AddressVO.from("Example.COM:8080")
        assertNotNull(result)
        assertEquals("Example.COM", result.host)
    }

    @Test
    fun `from parses localhost`() {
        val result = AddressVO.from("localhost:8080")
        assertNotNull(result)
        assertEquals("localhost", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `from parses IP address`() {
        val result = AddressVO.from("192.168.1.100:8080")
        assertNotNull(result)
        assertEquals("192.168.1.100", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `from trims whitespace`() {
        val result = AddressVO.from("  example.com:8080  ")
        assertNotNull(result)
        assertEquals("example.com", result.host)
    }

    @Test
    fun `from parses URL with path`() {
        val result = AddressVO.from("http://example.com:8080/api/v1")
        assertNotNull(result)
        assertEquals("example.com", result.host)
        assertEquals(8080, result.port)
    }

    @Test
    fun `toString returns host colon port`() {
        val address = AddressVO("example.com", 8080)
        assertEquals("example.com:8080", address.toString())
    }

    @Test
    fun `data class equality works`() {
        val addr1 = AddressVO("example.com", 8080)
        val addr2 = AddressVO("example.com", 8080)
        assertEquals(addr1, addr2)
    }

    @Test
    fun `data class copy works`() {
        val original = AddressVO("example.com", 8080)
        val copied = original.copy(port = 9090)
        assertEquals("example.com", copied.host)
        assertEquals(9090, copied.port)
    }

    @Test
    fun `from parses valid port boundaries`() {
        val result1 = AddressVO.from("example.com:1")
        assertNotNull(result1)
        assertEquals(1, result1.port)

        val result2 = AddressVO.from("example.com:65535")
        assertNotNull(result2)
        assertEquals(65535, result2.port)
    }
}
