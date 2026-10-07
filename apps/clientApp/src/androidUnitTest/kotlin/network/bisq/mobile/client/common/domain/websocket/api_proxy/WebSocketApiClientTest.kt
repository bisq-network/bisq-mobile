package network.bisq.mobile.client.common.domain.websocket.api_proxy

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.domain.utils.redactedSummary
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the cancellation handling in [WebSocketApiClient.executeRequest]: a [CancellationException]
 * raised while the calling coroutine is still active (e.g. the request timeout, which is a
 * TimeoutCancellationException) must be reported as a plain request failure rather than propagated —
 * only a genuine cancellation of the caller's own job should tear the caller down.
 */
class WebSocketApiClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `request reports a timeout-style cancellation as a failure while the caller is active`() =
        runTest {
            val webSocketClientService =
                mockk<WebSocketClientService> {
                    coEvery { sendRequestAndAwaitResponse(any()) } throws CancellationException("request timed out")
                }
            val client = WebSocketApiClient(webSocketClientService, json)

            val result = client.get<String>("some/path")

            // Caller job is still active -> ensureActive() is a no-op -> wrapped failure, as before.
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is CancellationException)
        }

    @Test
    fun `request propagates a genuine caller cancellation instead of returning a failure`() =
        runTest {
            // The request parks until the caller is cancelled, so ensureActive() observes a genuine
            // cancellation and must tear the caller down rather than hand back a Result.failure.
            val gate = CompletableDeferred<Unit>()
            val webSocketClientService =
                mockk<WebSocketClientService> {
                    coEvery { sendRequestAndAwaitResponse(any()) } coAnswers {
                        gate.await()
                        error("unreachable once cancelled")
                    }
                }
            val client = WebSocketApiClient(webSocketClientService, json)

            var outcome: Result<String>? = null
            val child =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    outcome = client.get<String>("some/path")
                }

            child.cancel()
            child.join()

            // Cancellation propagated out of get(); it never produced a (mis)reported failure result.
            assertNull(outcome)
        }

    @Test
    fun `an error response with a malformed json body becomes a rest failure carrying the status`() =
        runTest {
            val body = """{"error": broken"""
            val webSocketClientService =
                mockk<WebSocketClientService> {
                    coEvery { sendRequestAndAwaitResponse(any()) } returns
                        WebSocketRestApiResponse(requestId = "r", statusCode = HttpStatusCode.BadRequest.value, body = body)
                }

            val result = WebSocketApiClient(webSocketClientService, json).get<String>("some/path")

            val failure = result.exceptionOrNull() as WebSocketRestApiException
            assertEquals(HttpStatusCode.BadRequest, failure.httpStatusCode)
            // The body reaches the caller as the message, and only the status reaches a log line.
            assertEquals(body, failure.message)
            assertEquals("WebSocketRestApiException(http=400)", failure.redactedSummary())
        }

    @Test
    fun `a transport exception is reported as a plain failure`() =
        runTest {
            val webSocketClientService =
                mockk<WebSocketClientService> {
                    coEvery { sendRequestAndAwaitResponse(any()) } throws IllegalStateException("socket closed by peer")
                }

            val result = WebSocketApiClient(webSocketClientService, json).get<String>("some/path")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
        }
}
