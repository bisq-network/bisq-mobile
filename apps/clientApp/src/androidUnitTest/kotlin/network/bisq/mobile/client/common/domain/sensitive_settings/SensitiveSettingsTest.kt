package network.bisq.mobile.client.common.domain.sensitive_settings

import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SensitiveSettingsTest {
    @Test
    fun `default values are correct`() {
        val settings = SensitiveSettings()

        assertNull(settings.clientName)
        assertEquals("", settings.bisqApiUrl)
        assertNull(settings.tlsFingerprint)
        assertNull(settings.clientSecret)
        assertNull(settings.clientId)
        assertNull(settings.sessionId)
        assertEquals(BisqProxyOption.NONE, settings.selectedProxyOption)
        assertEquals("", settings.externalProxyUrl)
    }

    @Test
    fun `all properties can be set`() {
        val settings =
            SensitiveSettings(
                clientName = "MyClient",
                bisqApiUrl = "wss://example.com:8090",
                tlsFingerprint = "abc123",
                clientSecret = "secret",
                clientId = "client-id",
                sessionId = "session-id",
                selectedProxyOption = BisqProxyOption.INTERNAL_TOR,
                externalProxyUrl = "127.0.0.1:9050",
            )

        assertEquals("MyClient", settings.clientName)
        assertEquals("wss://example.com:8090", settings.bisqApiUrl)
        assertEquals("abc123", settings.tlsFingerprint)
        assertEquals("secret", settings.clientSecret)
        assertEquals("client-id", settings.clientId)
        assertEquals("session-id", settings.sessionId)
        assertEquals(BisqProxyOption.INTERNAL_TOR, settings.selectedProxyOption)
        assertEquals("127.0.0.1:9050", settings.externalProxyUrl)
    }

    @Test
    fun `data class equality works correctly`() {
        val settings1 = SensitiveSettings(clientName = "Test")
        val settings2 = SensitiveSettings(clientName = "Test")

        assertEquals(settings1, settings2)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = SensitiveSettings(clientName = "Original")
        val copied = original.copy(clientName = "Copied")

        assertEquals("Copied", copied.clientName)
        assertEquals("", copied.bisqApiUrl)
    }

    @Test
    fun `copy preserves other fields`() {
        val original =
            SensitiveSettings(
                clientName = "Test",
                bisqApiUrl = "wss://example.com",
                sessionId = "session-123",
            )
        val copied = original.copy(clientName = "NewName")

        assertEquals("NewName", copied.clientName)
        assertEquals("wss://example.com", copied.bisqApiUrl)
        assertEquals("session-123", copied.sessionId)
    }

    @Test
    fun `external tor proxy option can be set`() {
        val settings =
            SensitiveSettings(
                selectedProxyOption = BisqProxyOption.EXTERNAL_TOR,
                externalProxyUrl = "192.168.1.100:9050",
            )

        assertEquals(BisqProxyOption.EXTERNAL_TOR, settings.selectedProxyOption)
        assertEquals("192.168.1.100:9050", settings.externalProxyUrl)
    }

    @Test
    fun `socks proxy option can be set`() {
        val settings =
            SensitiveSettings(
                selectedProxyOption = BisqProxyOption.SOCKS_PROXY,
                externalProxyUrl = "proxy.example.com:1080",
            )

        assertEquals(BisqProxyOption.SOCKS_PROXY, settings.selectedProxyOption)
        assertEquals("proxy.example.com:1080", settings.externalProxyUrl)
    }
}
