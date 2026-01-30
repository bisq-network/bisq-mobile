package network.bisq.mobile.client.common.domain.websocket

import io.ktor.client.HttpClient
import io.ktor.http.parseUrl
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

        assertIs<WebSocketClientDemo>(factory.createNewClient(httpClient, parseUrl("http://$demoHost:$demoPort")!!))
    }

    @Test
    fun `creates impl client if demo host and port are NOT used`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)
        val demoHost = "foo.bar"
        val demoPort = 21

        assertIs<WebSocketClientImpl>(factory.createNewClient(httpClient, parseUrl("http://$demoHost:$demoPort")!!))
    }

    @Test
    fun `creates impl client for localhost`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)

        assertIs<WebSocketClientImpl>(factory.createNewClient(httpClient, parseUrl("http://localhost:8090")!!))
    }

    @Test
    fun `creates impl client for IP address`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)

        assertIs<WebSocketClientImpl>(factory.createNewClient(httpClient, parseUrl("http://192.168.1.100:8090")!!))
    }

    @Test
    fun `creates impl client for onion address`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)

        assertIs<WebSocketClientImpl>(
            factory.createNewClient(
                httpClient,
                parseUrl("http://abcdefghijklmnopqrstuvwxyz234567.onion:8090")!!,
            ),
        )
    }

    @Test
    fun `creates impl client when demo host but wrong port`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)

        // demo.bisq but port 8090 instead of 21
        assertIs<WebSocketClientImpl>(factory.createNewClient(httpClient, parseUrl("http://demo.bisq:8090")!!))
    }

    @Test
    fun `creates impl client when demo port but wrong host`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)

        // Port 21 but not demo.bisq host
        assertIs<WebSocketClientImpl>(factory.createNewClient(httpClient, parseUrl("http://example.com:21")!!))
    }

    @Test
    fun `creates impl client with session and client ids`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)

        val client =
            factory.createNewClient(
                httpClient,
                parseUrl("http://localhost:8090")!!,
                sessionId = "session-123",
                clientId = "client-456",
            )
        assertIs<WebSocketClientImpl>(client)
    }

    @Test
    fun `creates demo client ignores session and client ids`() {
        val json = mockk<Json>()
        val httpClient = mockk<HttpClient>()
        val factory = WebSocketClientFactory(json)

        val client =
            factory.createNewClient(
                httpClient,
                parseUrl("http://demo.bisq:21")!!,
                sessionId = "session-123",
                clientId = "client-456",
            )
        assertIs<WebSocketClientDemo>(client)
    }
}
