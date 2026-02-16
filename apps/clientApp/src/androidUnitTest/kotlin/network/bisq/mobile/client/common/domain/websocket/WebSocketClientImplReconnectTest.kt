package network.bisq.mobile.client.common.domain.websocket

import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.websocket.exception.MaximumRetryReachedException
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientImplReconnectTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope
    private lateinit var json: Json
    private val apiUrl = Url("http://localhost:8080")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher + SupervisorJob())
        json = Json { ignoreUnknownKeys = true }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createClient(): WebSocketClientImpl {
        val httpClient = mockk<HttpClient>(relaxed = true)
        return spyk(
            WebSocketClientImpl(
                httpClient = httpClient,
                json = json,
                apiUrl = apiUrl,
                sessionId = "session-id",
                clientId = "client-id",
                clientScope = testScope,
            ),
        )
    }

    @Test
    fun `isRetryableError returns false for UnauthorizedApiAccessException - no retry`() =
        runTest(testDispatcher) {
            // Given
            val client = createClient()
            var connectCallCount = 0
            coEvery { client.connect(any()) } answers {
                connectCallCount++
                UnauthorizedApiAccessException()
            }

            // When - call reconnect
            client.reconnect()
            testDispatcher.scheduler.advanceTimeBy(WebSocketClientImpl.DELAY_TO_RECONNECT + 100)
            testDispatcher.scheduler.runCurrent()

            // Advance extra time to see if a retry would be triggered (combines both test scenarios)
            testDispatcher.scheduler.advanceTimeBy(WebSocketClientImpl.DELAY_TO_RECONNECT * 10)
            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - connect should be called exactly once (no retry for 401)
            assertEquals(1, connectCallCount, "Expected exactly 1 connect attempt for UnauthorizedApiAccessException")
        }

    @Test
    fun `isRetryableError returns true for generic exceptions and retries`() =
        runTest(testDispatcher) {
            // Given
            val client = createClient()

            var connectCallCount = 0
            coEvery { client.connect(any()) } answers {
                connectCallCount++
                if (connectCallCount <= 2) {
                    RuntimeException("Connection error")
                } else {
                    null // Success on third attempt
                }
            }

            // When - call reconnect
            client.reconnect()
            testDispatcher.scheduler.advanceTimeBy(WebSocketClientImpl.DELAY_TO_RECONNECT + 100)
            testDispatcher.scheduler.runCurrent()

            // Advance through first retry
            testDispatcher.scheduler.advanceTimeBy(WebSocketClientImpl.DELAY_TO_RECONNECT * 2 + 100)
            testDispatcher.scheduler.runCurrent()

            // Advance through second retry
            testDispatcher.scheduler.advanceTimeBy(WebSocketClientImpl.DELAY_TO_RECONNECT * 4 + 100)
            testDispatcher.scheduler.runCurrent()

            delay(100)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - connect should be called multiple times (retries happen for generic exceptions)
            assertTrue(connectCallCount >= 2, "Expected at least 2 connect attempts, got $connectCallCount")
        }

    @Test
    fun `reconnect stops after MAX_RECONNECT_ATTEMPTS with retryable error`() =
        runTest(testDispatcher) {
            // Given
            val client = createClient()

            // Always return a retryable error
            coEvery { client.connect(any()) } returns RuntimeException("Retryable error")

            // When - call reconnect and advance through all retry attempts
            client.reconnect()

            for (attempt in 0 until WebSocketClientImpl.MAX_RECONNECT_ATTEMPTS) {
                val delayTime =
                    minOf(
                        WebSocketClientImpl.DELAY_TO_RECONNECT * (1 shl minOf(attempt, 4)),
                        WebSocketClientImpl.MAX_RECONNECT_DELAY,
                    )
                testDispatcher.scheduler.advanceTimeBy(delayTime + 100)
                testDispatcher.scheduler.runCurrent()
            }

            delay(100)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - final state should be Disconnected with MaximumRetryReachedException
            val state = client.webSocketClientStatus.value
            assertTrue(state is ConnectionState.Disconnected)
            val error = (state as ConnectionState.Disconnected).error
            // Enforce strict MaximumRetryReachedException - not just any RuntimeException
            assertTrue(
                error is MaximumRetryReachedException,
                "Expected MaximumRetryReachedException, got ${error?.javaClass?.simpleName}",
            )
        }

    @Test
    fun `reconnect resets counter after exceeding MAX_RECONNECT_ATTEMPTS`() =
        runTest(testDispatcher) {
            // Given
            val client = createClient()

            var connectCallCount = 0
            coEvery { client.connect(any()) } answers {
                connectCallCount++
                RuntimeException("Connection failed")
            }

            // When - trigger reconnect and exhaust attempts
            client.reconnect()

            for (attempt in 0 until WebSocketClientImpl.MAX_RECONNECT_ATTEMPTS) {
                val delayTime =
                    minOf(
                        WebSocketClientImpl.DELAY_TO_RECONNECT * (1 shl minOf(attempt, 4)),
                        WebSocketClientImpl.MAX_RECONNECT_DELAY,
                    )
                testDispatcher.scheduler.advanceTimeBy(delayTime + 100)
                testDispatcher.scheduler.runCurrent()
            }

            testDispatcher.scheduler.advanceUntilIdle()

            val firstSeriesCallCount = connectCallCount

            // Then - verify max attempts were made
            assertTrue(firstSeriesCallCount >= WebSocketClientImpl.MAX_RECONNECT_ATTEMPTS)

            // Reset counter
            connectCallCount = 0

            // When - trigger reconnect again (counter should be reset)
            client.reconnect()

            for (attempt in 0..1) {
                val delayTime =
                    minOf(
                        WebSocketClientImpl.DELAY_TO_RECONNECT * (1 shl minOf(attempt, 4)),
                        WebSocketClientImpl.MAX_RECONNECT_DELAY,
                    )
                testDispatcher.scheduler.advanceTimeBy(delayTime + 100)
                testDispatcher.scheduler.runCurrent()
            }

            testDispatcher.scheduler.advanceUntilIdle()

            // Then - should be able to retry again (counter was reset)
            assertTrue(connectCallCount >= 1)
        }

    @Test
    fun `reconnect skips if already reconnecting`() =
        runTest(testDispatcher) {
            // Given
            val client = createClient()

            var connectCallCount = 0
            coEvery { client.connect(any()) } coAnswers {
                connectCallCount++
                delay(5000) // Long delay to keep reconnecting state
                null
            }

            // When - call reconnect twice rapidly
            client.reconnect()
            testDispatcher.scheduler.advanceTimeBy(100)
            testDispatcher.scheduler.runCurrent()

            client.reconnect() // Second call should be ignored

            testDispatcher.scheduler.advanceTimeBy(WebSocketClientImpl.DELAY_TO_RECONNECT + 100)
            testDispatcher.scheduler.runCurrent()

            // Then - connect should be called only once
            assertEquals(1, connectCallCount)
        }
}
