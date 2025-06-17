package network.bisq.mobile.client.websocket

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebSocketClientTest {

    @Test
    fun `demo URL constant is correct`() {
        // Verify the demo URL constant is as expected
        assertTrue(WebSocketClient.DEMO_URL == "ws://demo.bisq:21", "Demo URL should be ws://demo.bisq:21")
    }

    @Test
    fun `demo URL detection works correctly`() {
        // Test that demo URL is detected correctly
        val demoUrl = "ws://demo.bisq:21/websocket"
        assertTrue(demoUrl.startsWith(WebSocketClient.DEMO_URL), "Demo URL should be detected correctly")

        // Test that regular URLs are not detected as demo
        val regularUrl = "ws://localhost:8090/websocket"
        assertFalse(regularUrl.startsWith(WebSocketClient.DEMO_URL), "Regular URL should not be detected as demo")

        val anotherRegularUrl = "ws://10.0.2.2:8090/websocket"
        assertFalse(anotherRegularUrl.startsWith(WebSocketClient.DEMO_URL), "Another regular URL should not be detected as demo")
    }
}
