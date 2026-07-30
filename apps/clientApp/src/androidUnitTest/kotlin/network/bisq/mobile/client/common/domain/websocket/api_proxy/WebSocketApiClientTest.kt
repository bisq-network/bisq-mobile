package network.bisq.mobile.client.common.domain.websocket.api_proxy

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import org.junit.Test
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
}
