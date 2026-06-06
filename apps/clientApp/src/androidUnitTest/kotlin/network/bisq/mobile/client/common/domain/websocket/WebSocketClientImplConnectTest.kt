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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.access.utils.Headers
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientImplConnectTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var testScope: TestScope
    private lateinit var json: Json
    private val apiUrl = Url("http://localhost:8080")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher + SupervisorJob())
        json = Json { ignoreUnknownKeys = true }
        mockkStatic("io.ktor.client.plugins.websocket.BuildersKt")
    }

    @After
    fun tearDown() {
        unmockkStatic("io.ktor.client.plugins.websocket.BuildersKt")
        Dispatchers.resetMain()
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
        runTest(testDispatcher) {
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
