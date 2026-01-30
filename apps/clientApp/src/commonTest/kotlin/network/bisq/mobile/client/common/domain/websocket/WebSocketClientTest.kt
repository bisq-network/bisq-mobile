package network.bisq.mobile.client.common.domain.websocket

import kotlin.test.Test
import kotlin.test.assertEquals

class WebSocketClientTest {
    @Test
    fun `determineTimeout returns TOR_CONNECT_TIMEOUT for onion host`() {
        val timeout = WebSocketClient.determineTimeout("abcdefghijklmnop.onion")
        assertEquals(WebSocketClient.TOR_CONNECT_TIMEOUT, timeout)
    }

    @Test
    fun `determineTimeout returns CLEARNET_CONNECT_TIMEOUT for regular host`() {
        val timeout = WebSocketClient.determineTimeout("example.com")
        assertEquals(WebSocketClient.CLEARNET_CONNECT_TIMEOUT, timeout)
    }

    @Test
    fun `determineTimeout returns CLEARNET_CONNECT_TIMEOUT for localhost`() {
        val timeout = WebSocketClient.determineTimeout("localhost")
        assertEquals(WebSocketClient.CLEARNET_CONNECT_TIMEOUT, timeout)
    }

    @Test
    fun `determineTimeout returns CLEARNET_CONNECT_TIMEOUT for IP address`() {
        val timeout = WebSocketClient.determineTimeout("192.168.1.100")
        assertEquals(WebSocketClient.CLEARNET_CONNECT_TIMEOUT, timeout)
    }

    @Test
    fun `determineTimeout returns TOR_CONNECT_TIMEOUT for v3 onion address`() {
        val v3Onion = "uhw224s7asl3m43p7kzdk2yoflweswq54zmnzvnsnrxucnlamdltx6id.onion"
        val timeout = WebSocketClient.determineTimeout(v3Onion)
        assertEquals(WebSocketClient.TOR_CONNECT_TIMEOUT, timeout)
    }

    @Test
    fun `CLEARNET_CONNECT_TIMEOUT is 15 seconds`() {
        assertEquals(15_000L, WebSocketClient.CLEARNET_CONNECT_TIMEOUT)
    }

    @Test
    fun `TOR_CONNECT_TIMEOUT is 60 seconds`() {
        assertEquals(60_000L, WebSocketClient.TOR_CONNECT_TIMEOUT)
    }

    @Test
    fun `determineTimeout is case sensitive for onion suffix`() {
        // .ONION (uppercase) should not match
        val timeout = WebSocketClient.determineTimeout("example.ONION")
        assertEquals(WebSocketClient.CLEARNET_CONNECT_TIMEOUT, timeout)
    }
}
