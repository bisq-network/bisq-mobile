package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertIs

class WebSocketClientFactoryTest {

    @Test
    fun `creates demo client if demo host and port are used`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)
        val demoHost = "demo.bisq"
        val demoPort = 21

        assertIs<WebSocketClientDemo>(factory.createNewClient(httpClient, demoHost, demoPort))
    }

    @Test
    fun `creates impl client if demo host and port are NOT used`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)
        val demoHost = "foo.bar"
        val demoPort = 21

        assertIs<WebSocketClientImpl>(factory.createNewClient(httpClient, demoHost, demoPort))
    }
}
