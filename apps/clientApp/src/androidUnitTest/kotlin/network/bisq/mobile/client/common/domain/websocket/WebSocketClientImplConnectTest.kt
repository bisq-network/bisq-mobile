package network.bisq.mobile.client.common.domain.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.access.utils.Headers
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientImplConnectTest : ClientKoinIntegrationTestBase() {
    private val testScope = TestScope(testDispatcher + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    private val apiUrl = Url("http://localhost:8080")

    override fun onSetup() {
        mockkStatic("io.ktor.client.plugins.websocket.BuildersKt")
    }

    override fun onTearDown() {
        try {
            unmockkStatic("io.ktor.client.plugins.websocket.BuildersKt")
        } finally {
            super.onTearDown()
        }
    }

    private fun createClient(httpClient: HttpClient): WebSocketClientImpl =
        WebSocketClientImpl(
            httpClient = httpClient,
            json = json,
            apiUrl = apiUrl,
            sessionId = "session-id",
            clientId = "client-id",
            clientScope = testScope,
        )

    @Test
    fun `connect sends session credentials on upgrade and reports TCP upgrade failure`() =
        runTest {
            val httpClient = mockk<HttpClient>()
            val requestConfig = slot<HttpRequestBuilder.() -> Unit>()
            coEvery {
                httpClient.webSocketSession(capture(requestConfig))
            } coAnswers {
                val builder = HttpRequestBuilder()
                requestConfig.captured.invoke(builder)
                assertEquals("session-id", builder.headers[Headers.SESSION_ID])
                assertEquals("client-id", builder.headers[Headers.CLIENT_ID])
                throw RuntimeException("TCP upgrade failed")
            }

            val client = createClient(httpClient)
            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(error is RuntimeException)
            assertTrue(client.webSocketClientStatus.value is ConnectionState.Disconnected)
        }
}
