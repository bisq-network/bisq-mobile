package network.bisq.mobile.client.common.domain.httpclient

import io.ktor.client.engine.ProxyBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BisqProxyConfigTest {
    @Test
    fun `properties are accessible`() {
        val proxyConfig = ProxyBuilder.socks("127.0.0.1", 9050)
        val config = BisqProxyConfig(proxyConfig, true)

        assertEquals(proxyConfig, config.config)
        assertTrue(config.isTorProxy)
    }

    @Test
    fun `isTorProxy can be false`() {
        val proxyConfig = ProxyBuilder.socks("proxy.example.com", 1080)
        val config = BisqProxyConfig(proxyConfig, false)

        assertFalse(config.isTorProxy)
    }

    @Test
    fun `data class equality works`() {
        val proxyConfig = ProxyBuilder.socks("127.0.0.1", 9050)
        val config1 = BisqProxyConfig(proxyConfig, true)
        val config2 = BisqProxyConfig(proxyConfig, true)

        assertEquals(config1, config2)
    }

    @Test
    fun `data class copy works`() {
        val proxyConfig = ProxyBuilder.socks("127.0.0.1", 9050)
        val original = BisqProxyConfig(proxyConfig, true)
        val copied = original.copy(isTorProxy = false)

        assertEquals(proxyConfig, copied.config)
        assertFalse(copied.isTorProxy)
    }

    @Test
    fun `different ports create different configs`() {
        val proxyConfig1 = ProxyBuilder.socks("127.0.0.1", 9050)
        val proxyConfig2 = ProxyBuilder.socks("127.0.0.1", 9051)

        val config1 = BisqProxyConfig(proxyConfig1, true)
        val config2 = BisqProxyConfig(proxyConfig2, true)

        // The configs should be different because the underlying ProxyConfig is different
        assertTrue(config1 != config2)
    }
}
