package network.bisq.mobile.client.common.domain.httpclient

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BisqProxyOptionTest {
    @Test
    fun `NONE is not a tor proxy option`() {
        assertFalse(BisqProxyOption.NONE.isTorProxyOption)
    }

    @Test
    fun `INTERNAL_TOR is a tor proxy option`() {
        assertTrue(BisqProxyOption.INTERNAL_TOR.isTorProxyOption)
    }

    @Test
    fun `EXTERNAL_TOR is a tor proxy option`() {
        assertTrue(BisqProxyOption.EXTERNAL_TOR.isTorProxyOption)
    }

    @Test
    fun `SOCKS_PROXY is not a tor proxy option`() {
        assertFalse(BisqProxyOption.SOCKS_PROXY.isTorProxyOption)
    }

    @Test
    fun `all enum values are defined`() {
        val values = BisqProxyOption.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(BisqProxyOption.NONE))
        assertTrue(values.contains(BisqProxyOption.INTERNAL_TOR))
        assertTrue(values.contains(BisqProxyOption.EXTERNAL_TOR))
        assertTrue(values.contains(BisqProxyOption.SOCKS_PROXY))
    }

    @Test
    fun `enum values have correct ordinals`() {
        assertEquals(0, BisqProxyOption.NONE.ordinal)
        assertEquals(1, BisqProxyOption.INTERNAL_TOR.ordinal)
        assertEquals(2, BisqProxyOption.EXTERNAL_TOR.ordinal)
        assertEquals(3, BisqProxyOption.SOCKS_PROXY.ordinal)
    }

    @Test
    fun `valueOf returns correct enum`() {
        assertEquals(BisqProxyOption.NONE, BisqProxyOption.valueOf("NONE"))
        assertEquals(BisqProxyOption.INTERNAL_TOR, BisqProxyOption.valueOf("INTERNAL_TOR"))
        assertEquals(BisqProxyOption.EXTERNAL_TOR, BisqProxyOption.valueOf("EXTERNAL_TOR"))
        assertEquals(BisqProxyOption.SOCKS_PROXY, BisqProxyOption.valueOf("SOCKS_PROXY"))
    }
}
