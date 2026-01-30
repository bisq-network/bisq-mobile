package network.bisq.mobile.client.common.domain.httpclient

import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpClientSettingsTest {
    @Test
    fun `default values are correct`() {
        val settings =
            HttpClientSettings(
                bisqApiUrl = "wss://example.com:8090",
                tlsFingerprint = null,
            )

        assertNull(settings.clientId)
        assertNull(settings.sessionId)
        assertEquals(BisqProxyOption.NONE, settings.selectedProxyOption)
        assertNull(settings.externalProxyUrl)
        assertFalse(settings.isTorProxy)
    }

    @Test
    fun `all properties can be set`() {
        val settings =
            HttpClientSettings(
                bisqApiUrl = "wss://example.com:8090",
                tlsFingerprint = "abc123",
                clientId = "client-id",
                sessionId = "session-id",
                selectedProxyOption = BisqProxyOption.INTERNAL_TOR,
                externalProxyUrl = "127.0.0.1:9050",
                isTorProxy = true,
            )

        assertEquals("wss://example.com:8090", settings.bisqApiUrl)
        assertEquals("abc123", settings.tlsFingerprint)
        assertEquals("client-id", settings.clientId)
        assertEquals("session-id", settings.sessionId)
        assertEquals(BisqProxyOption.INTERNAL_TOR, settings.selectedProxyOption)
        assertEquals("127.0.0.1:9050", settings.externalProxyUrl)
        assertTrue(settings.isTorProxy)
    }

    @Test
    fun `from creates settings with NONE proxy option`() {
        val sensitiveSettings =
            SensitiveSettings(
                bisqApiUrl = "wss://example.com:8090",
                tlsFingerprint = "fingerprint",
                clientId = "client",
                sessionId = "session",
                selectedProxyOption = BisqProxyOption.NONE,
            )

        val httpSettings = HttpClientSettings.from(sensitiveSettings, null)

        assertEquals("wss://example.com:8090", httpSettings.bisqApiUrl)
        assertEquals("fingerprint", httpSettings.tlsFingerprint)
        assertEquals("client", httpSettings.clientId)
        assertEquals("session", httpSettings.sessionId)
        assertEquals(BisqProxyOption.NONE, httpSettings.selectedProxyOption)
        assertNull(httpSettings.externalProxyUrl)
        assertFalse(httpSettings.isTorProxy)
    }

    @Test
    fun `from creates settings with INTERNAL_TOR proxy option`() {
        val sensitiveSettings =
            SensitiveSettings(
                bisqApiUrl = "wss://example.com:8090",
                selectedProxyOption = BisqProxyOption.INTERNAL_TOR,
            )

        val httpSettings = HttpClientSettings.from(sensitiveSettings, 9050)

        assertEquals(BisqProxyOption.INTERNAL_TOR, httpSettings.selectedProxyOption)
        assertEquals("127.0.0.1:9050", httpSettings.externalProxyUrl)
        assertTrue(httpSettings.isTorProxy)
    }

    @Test
    fun `from creates settings with EXTERNAL_TOR proxy option`() {
        val sensitiveSettings =
            SensitiveSettings(
                bisqApiUrl = "wss://example.com:8090",
                selectedProxyOption = BisqProxyOption.EXTERNAL_TOR,
                externalProxyUrl = "192.168.1.100:9050",
            )

        val httpSettings = HttpClientSettings.from(sensitiveSettings, null)

        assertEquals(BisqProxyOption.EXTERNAL_TOR, httpSettings.selectedProxyOption)
        assertEquals("192.168.1.100:9050", httpSettings.externalProxyUrl)
        assertTrue(httpSettings.isTorProxy)
    }

    @Test
    fun `from creates settings with SOCKS_PROXY option`() {
        val sensitiveSettings =
            SensitiveSettings(
                bisqApiUrl = "wss://example.com:8090",
                selectedProxyOption = BisqProxyOption.SOCKS_PROXY,
                externalProxyUrl = "proxy.example.com:1080",
            )

        val httpSettings = HttpClientSettings.from(sensitiveSettings, null)

        assertEquals(BisqProxyOption.SOCKS_PROXY, httpSettings.selectedProxyOption)
        assertEquals("proxy.example.com:1080", httpSettings.externalProxyUrl)
        assertFalse(httpSettings.isTorProxy)
    }

    @Test
    fun `bisqProxyConfig returns null when no proxy URL`() {
        val settings =
            HttpClientSettings(
                bisqApiUrl = "wss://example.com:8090",
                tlsFingerprint = null,
                externalProxyUrl = null,
            )

        assertNull(settings.bisqProxyConfig())
    }

    @Test
    fun `bisqProxyConfig returns null for empty proxy URL`() {
        val settings =
            HttpClientSettings(
                bisqApiUrl = "wss://example.com:8090",
                tlsFingerprint = null,
                externalProxyUrl = "",
            )

        assertNull(settings.bisqProxyConfig())
    }

    @Test
    fun `bisqProxyConfig returns config for valid proxy URL`() {
        val settings =
            HttpClientSettings(
                bisqApiUrl = "wss://example.com:8090",
                tlsFingerprint = null,
                externalProxyUrl = "127.0.0.1:9050",
                isTorProxy = true,
            )

        val config = settings.bisqProxyConfig()
        assertNotNull(config)
        assertTrue(config.isTorProxy)
    }

    @Test
    fun `data class equality works`() {
        val settings1 = HttpClientSettings("url", "fp")
        val settings2 = HttpClientSettings("url", "fp")
        assertEquals(settings1, settings2)
    }

    @Test
    fun `data class copy works`() {
        val original = HttpClientSettings("url", "fp")
        val copied = original.copy(bisqApiUrl = "newUrl")
        assertEquals("newUrl", copied.bisqApiUrl)
        assertEquals("fp", copied.tlsFingerprint)
    }
}
