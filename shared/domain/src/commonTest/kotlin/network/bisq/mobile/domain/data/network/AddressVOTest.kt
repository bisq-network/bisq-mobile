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
        val result = AddressVO.from("http://test.onion:80")
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
    fun `from should return null for malformed URL`() {
        val result = AddressVO.from("not-a-url")
        assertNull(result)
    }

    @Test
    fun `from should return null for URL without host`() {
        val result = AddressVO.from("http://:8080")
        assertNull(result)
    }
}
