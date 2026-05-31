package network.bisq.mobile.client.common.domain.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.http.Url
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientImplConnectTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope
    private lateinit var json: Json
    private val apiUrl = Url("http://localhost:8080")

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher + SupervisorJob())
        json = Json { ignoreUnknownKeys = true }
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
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

    private fun setConnectionStatus(
        client: WebSocketClientImpl,
        state: ConnectionState,
    ) {
        val field = WebSocketClientImpl::class.java.getDeclaredField("_webSocketClientStatus")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(client) as MutableStateFlow<ConnectionState>).value = state
    }

    private fun setSession(
        client: WebSocketClientImpl,
        session: DefaultClientWebSocketSession?,
    ) {
        val field = WebSocketClientImpl::class.java.getDeclaredField("session")
        field.isAccessible = true
        field.set(client, session)
    }

    @Test
    fun `connect returns early when status and session are both live`() =
        runTest(testDispatcher) {
            val httpClient = mockk<HttpClient>(relaxed = true)
            val client = createClient(httpClient)
            val liveSession = mockk<DefaultClientWebSocketSession>(relaxed = true)
            io.mockk.every { liveSession.isActive } returns true

            setConnectionStatus(client, ConnectionState.Connected)
            setSession(client, liveSession)

            assertNull(client.connect(5_000))
        }

    @Test
    fun `connect clears stale Connected status when session is absent`() =
        runTest(testDispatcher) {
            val httpClient = mockk<HttpClient>(relaxed = true)
            val client = createClient(httpClient)
            setConnectionStatus(client, ConnectionState.Connected)

            client.connect(5_000)

            assertTrue(client.webSocketClientStatus.value is ConnectionState.Disconnected)
        }
}
