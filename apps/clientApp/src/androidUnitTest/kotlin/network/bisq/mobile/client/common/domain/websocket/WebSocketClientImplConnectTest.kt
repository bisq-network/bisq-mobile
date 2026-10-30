package network.bisq.mobile.client.common.domain.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.Url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.client.common.di.clientJson
import network.bisq.mobile.client.common.domain.access.utils.Headers
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.client.common.domain.websocket.exception.WebSocketSessionClosedEarly
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.shared.BuildConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientImplConnectTest : ClientKoinIntegrationTestBase() {
    private val testScope = TestScope(testDispatcher + SupervisorJob())
    private val json = clientJson
    private val apiUrl = Url("http://localhost:8080")
    private val compatibleVersion = BuildConfig.BISQ_API_VERSION

    override fun onSetup() {
        mockkStatic("io.ktor.client.plugins.websocket.BuildersKt")
    }

    override fun onTearDown() {
        try {
            // Clients run their listeners in testScope; cancelling it disposes them even when a test fails.
            testScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()
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

    /**
     * A node behind a fake WS session: every REST request proxied over the socket is answered
     * from [responses] (path -> status and body; unknown paths get 404). Requested paths are
     * recorded in [requestedPaths] and the headers of each request in [requestedHeaders].
     *
     * Paths in [credentialsReadFromMessage] answer 401 unless the message carries the session and
     * client headers, as a node before bisq-api 2.1.14 does.
     *
     * Responses wait for [responsesReleased] when it is set, like a slow handshake. With
     * [closeAfterResponse] the node closes the session right after its first response.
     */
    private inner class FakeNode(
        private val responses: Map<String, Pair<Int, String>>,
        private val credentialsReadFromMessage: Set<String> = emptySet(),
        private val responsesReleased: CompletableDeferred<Unit>? = null,
        private val closeAfterResponse: Boolean = false,
    ) {
        val requestedPaths = mutableListOf<String>()
        val requestedHeaders = mutableListOf<Map<String, String>>()
        var sessionsOpened = 0
            private set
        val httpClient = mockk<HttpClient>()

        /** Frames of the current session; closing it simulates a server-side close. */
        lateinit var incoming: Channel<Frame>
            private set

        init {
            coEvery { httpClient.webSocketSession(any<HttpRequestBuilder.() -> Unit>()) } answers { newSession() }
        }

        private fun newSession(): DefaultClientWebSocketSession {
            sessionsOpened++
            val channel = Channel<Frame>(Channel.UNLIMITED)
            incoming = channel
            val session = mockk<DefaultClientWebSocketSession>(relaxed = true)
            every { session.coroutineContext } returns Job()
            every { session.incoming } returns channel
            coEvery { session.send(any()) } coAnswers {
                val frame = firstArg<Frame>()
                if (frame is Frame.Text) {
                    val request = json.decodeFromString(WebSocketMessage.serializer(), frame.readText()) as WebSocketRestApiRequest
                    requestedPaths += request.path
                    requestedHeaders += request.headers
                    val hasCredentials = Headers.SESSION_ID in request.headers && Headers.CLIENT_ID in request.headers
                    val (status, body) =
                        if (request.path in credentialsReadFromMessage && !hasCredentials) {
                            401 to ""
                        } else {
                            responses[request.path] ?: (404 to "")
                        }
                    val response = WebSocketRestApiResponse(request.requestId, status, body)
                    responsesReleased?.await()
                    channel.send(Frame.Text(json.encodeToString(WebSocketMessage.serializer(), response)))
                    if (closeAfterResponse) channel.close()
                } else {
                    channel.close()
                }
            }
            return session
        }
    }

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

    @Test
    fun `connect publishes the node info from GET info before reporting Connected`() =
        runTest {
            val node =
                FakeNode(
                    mapOf(
                        WebSocketClientImpl.INFO_PATH to
                            (200 to """{"version":"$compatibleVersion","imageVersion":"$compatibleVersion.2","features":["closed-trades"]}"""),
                    ),
                )
            val client = createClient(node.httpClient)
            var nodeInfoWhenConnected: ApiInfoDto? = null
            // Unconfined so the observer runs inline when Connected is set, not after connect() returns.
            val observer =
                launch(UnconfinedTestDispatcher(testDispatcher.scheduler)) {
                    client.webSocketClientStatus.first { it is ConnectionState.Connected }
                    nodeInfoWhenConnected = client.nodeInfo.value
                }

            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(error)
            val expected = ApiInfoDto(compatibleVersion, "$compatibleVersion.2", listOf("closed-trades"))
            assertEquals(expected, nodeInfoWhenConnected)
            assertEquals(listOf(WebSocketClientImpl.INFO_PATH), node.requestedPaths)
        }

    @Test
    fun `connect falls back to settings version when the node has no GET info`() =
        runTest {
            val node =
                FakeNode(
                    mapOf(
                        WebSocketClientImpl.HEALTH_CHECK_PATH to
                            (200 to """{"version":"$compatibleVersion","imageVersion":"$compatibleVersion.1"}"""),
                    ),
                )
            val client = createClient(node.httpClient)

            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(error)
            assertTrue(client.webSocketClientStatus.value is ConnectionState.Connected)
            assertEquals(ApiInfoDto(compatibleVersion, "$compatibleVersion.1", features = null), client.nodeInfo.value)
            assertEquals(listOf(WebSocketClientImpl.INFO_PATH, WebSocketClientImpl.HEALTH_CHECK_PATH), node.requestedPaths)
        }

    @Test
    fun `a second connect on a node without GET info skips the probe`() =
        runTest {
            val node =
                FakeNode(
                    mapOf(
                        WebSocketClientImpl.HEALTH_CHECK_PATH to (200 to """{"version":"$compatibleVersion"}"""),
                    ),
                )
            val client = createClient(node.httpClient)

            client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()
            client.disconnect()
            testDispatcher.scheduler.advanceUntilIdle()
            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(error)
            assertEquals(
                listOf(WebSocketClientImpl.INFO_PATH, WebSocketClientImpl.HEALTH_CHECK_PATH, WebSocketClientImpl.HEALTH_CHECK_PATH),
                node.requestedPaths,
            )
        }

    @Test
    fun `a server close clears the node info`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"$compatibleVersion","features":[]}""")))
            val client = createClient(node.httpClient)
            client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(client.nodeInfo.value)

            node.incoming.close()
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(client.webSocketClientStatus.value is ConnectionState.Disconnected)
            assertNull(client.nodeInfo.value)
        }

    @Test
    fun `disconnect clears the node info`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"$compatibleVersion","features":[]}""")))
            val client = createClient(node.httpClient)
            client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull(client.nodeInfo.value)

            client.disconnect()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(client.nodeInfo.value)
        }

    @Test
    fun `disconnect keeps the node info until the status reports Disconnected`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"$compatibleVersion","features":[]}""")))
            val client = createClient(node.httpClient)
            client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            // The listener has not processed the cancellation yet: the status is still Connected.
            client.disconnect()
            assertTrue(client.webSocketClientStatus.value is ConnectionState.Connected)
            assertNotNull(client.nodeInfo.value)

            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(client.webSocketClientStatus.value is ConnectionState.Disconnected)
            assertNull(client.nodeInfo.value)
        }

    @Test
    fun `connect names the failing path when GET info answers a server error`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (500 to "")))
            val client = createClient(node.httpClient)

            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(error is IllegalStateException)
            assertTrue(error.message!!.contains(WebSocketClientImpl.INFO_PATH), error.message)
            assertNull(client.nodeInfo.value)
        }

    @Test
    fun `connect names the failing path when the settings version fallback answers a server error`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.HEALTH_CHECK_PATH to (500 to "")))
            val client = createClient(node.httpClient)

            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(error is IllegalStateException)
            assertTrue(error.message!!.contains(WebSocketClientImpl.HEALTH_CHECK_PATH), error.message)
            assertNull(client.nodeInfo.value)
        }

    @Test
    fun `connect rejects an incompatible node version and leaves node info unset`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"0.0.1","features":[]}""")))
            val client = createClient(node.httpClient)

            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(error is IncompatibleHttpApiVersionException)
            assertEquals("0.0.1", error.serverVersion)
            assertNull(client.nodeInfo.value)
            assertTrue(client.webSocketClientStatus.value is ConnectionState.Disconnected)
        }

    @Test
    fun `connect after an incompatible node version returns the same error without a new handshake`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"0.0.1","features":[]}""")))
            val client = createClient(node.httpClient)
            val firstError = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            val secondError = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertSame(firstError, secondError)
            assertEquals(listOf(WebSocketClientImpl.INFO_PATH), node.requestedPaths)
            assertEquals(ConnectionState.Disconnected(firstError), client.webSocketClientStatus.value)
        }

    @Test
    fun `disconnect after an incompatible node version lets the next connect handshake again`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"0.0.1","features":[]}""")))
            val client = createClient(node.httpClient)
            client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            client.disconnect()
            client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(WebSocketClientImpl.INFO_PATH, WebSocketClientImpl.INFO_PATH), node.requestedPaths)
        }

    @Test
    fun `reconnect after an incompatible node version neither handshakes nor replaces the error`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"0.0.1","features":[]}""")))
            val client = createClient(node.httpClient)
            val firstError = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            // Past MAX_RECONNECT_ATTEMPTS the counter would replace the marker and the next call would handshake
            repeat(WebSocketClientImpl.MAX_RECONNECT_ATTEMPTS + 2) {
                client.reconnect()
                testDispatcher.scheduler.advanceUntilIdle()
            }

            assertEquals(listOf(WebSocketClientImpl.INFO_PATH), node.requestedPaths)
            assertEquals(ConnectionState.Disconnected(firstError), client.webSocketClientStatus.value)
        }

    @Test
    fun `reconnect after disconnect of an incompatible node handshakes again`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"0.0.1","features":[]}""")))
            val client = createClient(node.httpClient)
            client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            client.disconnect()
            client.reconnect()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(WebSocketClientImpl.INFO_PATH, WebSocketClientImpl.INFO_PATH), node.requestedPaths)
        }

    @Test
    fun `connect reports unauthorized when GET info answers 401`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (401 to "")))
            val client = createClient(node.httpClient)

            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(error is UnauthorizedApiAccessException)
            assertNull(client.nodeInfo.value)
            assertEquals(listOf(WebSocketClientImpl.INFO_PATH), node.requestedPaths)
        }

    /**
     * A node before bisq-api 2.1.14 reads the credentials of a proxied REST call from the message, not
     * from the upgrade request. Its version check must still reach it, so it is refused with the version
     * error rather than with a 401 that a session renewal can never fix.
     */
    @Test
    fun `a node that reads credentials from the message is refused for its version, not as unauthorized`() =
        runTest {
            val node =
                FakeNode(
                    responses =
                        mapOf(
                            WebSocketClientImpl.HEALTH_CHECK_PATH to (200 to """{"version":"2.1.13"}"""),
                        ),
                    credentialsReadFromMessage = setOf(WebSocketClientImpl.HEALTH_CHECK_PATH),
                )
            val client = createClient(node.httpClient)

            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(error is IncompatibleHttpApiVersionException)
            assertEquals("2.1.13", error.serverVersion)
        }

    @Test
    fun `only the version check carries the session and client headers`() =
        runTest {
            val offersPath = "/api/v1/offerbook/markets"
            val node =
                FakeNode(
                    mapOf(
                        WebSocketClientImpl.INFO_PATH to (200 to """{"version":"$compatibleVersion","features":[]}"""),
                        offersPath to (200 to "[]"),
                    ),
                )
            val client = createClient(node.httpClient)
            assertNull(client.connect(timeout = 5_000L))
            testDispatcher.scheduler.advanceUntilIdle()

            client.sendRequestAndAwaitResponse(WebSocketRestApiRequest("request-1", "GET", offersPath, ""), awaitConnection = false)

            assertEquals(listOf(WebSocketClientImpl.INFO_PATH, offersPath), node.requestedPaths)
            assertEquals(
                mapOf(Headers.SESSION_ID to "session-id", Headers.CLIENT_ID to "client-id"),
                node.requestedHeaders[0],
            )
            assertEquals(emptyMap(), node.requestedHeaders[1])
        }

    @Test
    fun `a reconnect during the handshake keeps the connection that the handshake completes`() =
        runTest {
            val responsesReleased = CompletableDeferred<Unit>()
            val node =
                FakeNode(
                    mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"$compatibleVersion","features":[]}""")),
                    responsesReleased = responsesReleased,
                )
            val client = createClient(node.httpClient)
            launch { client.connect(timeout = 5_000L) }
            testDispatcher.scheduler.runCurrent()
            assertEquals(ConnectionState.Connecting, client.webSocketClientStatus.value)

            // The health cycle sees Connecting as not connected and asks for a reconnect
            client.reconnect()
            testDispatcher.scheduler.runCurrent()
            responsesReleased.complete(Unit)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(ConnectionState.Connected, client.webSocketClientStatus.value)
            assertEquals(1, node.sessionsOpened)
            val response =
                client.sendRequestAndAwaitResponse(
                    WebSocketRestApiRequest("request-1", "GET", WebSocketClientImpl.INFO_PATH, ""),
                    awaitConnection = false,
                )
            assertTrue(response is WebSocketRestApiResponse)
        }

    @Test
    fun `a reconnect while connected replaces the session`() =
        runTest {
            val node = FakeNode(mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"$compatibleVersion","features":[]}""")))
            val client = createClient(node.httpClient)
            assertNull(client.connect(timeout = 5_000L))
            testDispatcher.scheduler.advanceUntilIdle()

            // A failed health check on a connection that still reports Connected
            client.reconnect()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, node.sessionsOpened)
            assertEquals(ConnectionState.Connected, client.webSocketClientStatus.value)
        }

    @Test
    fun `a session that closes after the handshake response is not reported Connected`() =
        runTest {
            val node =
                FakeNode(
                    mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"$compatibleVersion","features":[]}""")),
                    closeAfterResponse = true,
                )
            val client = createClient(node.httpClient)

            val error = client.connect(timeout = 5_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(error is WebSocketSessionClosedEarly, "Expected WebSocketSessionClosedEarly, got $error")
            assertTrue(client.webSocketClientStatus.value is ConnectionState.Disconnected)
        }

    @Test
    fun `a request without a session fails at once instead of timing out`() =
        runTest {
            val client = createClient(mockk(relaxed = true))

            assertFailsWith<WebSocketSessionClosedEarly> {
                client.sendRequestAndAwaitResponse(
                    WebSocketRestApiRequest("request-1", "GET", WebSocketClientImpl.INFO_PATH, ""),
                    awaitConnection = false,
                )
            }
        }
}
