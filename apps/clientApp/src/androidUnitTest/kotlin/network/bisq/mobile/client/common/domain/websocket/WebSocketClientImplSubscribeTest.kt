package network.bisq.mobile.client.common.domain.websocket

import io.ktor.http.Url
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.common.di.clientJson
import network.bisq.mobile.client.common.domain.websocket.exception.SubscribeTimeoutException
import network.bisq.mobile.client.common.domain.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.shared.BuildConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [WebSocketClientImpl.subscribe] against a [FakeWebSocketNode]: bisq2 registers the subscriber before it
 * builds the snapshot, so events can reach the client before the response. The collector starts before
 * the subscribe, as on startup, and records `subscriberId to sequenceNumber`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientImplSubscribeTest : ClientKoinIntegrationTestBase() {
    private val testScope = TestScope(testDispatcher + SupervisorJob())
    private val json = clientJson

    // Lazy: the fake session is stubbed on the static mock that onSetup installs.
    private val node by lazy {
        FakeWebSocketNode(
            json,
            mapOf(WebSocketClientImpl.INFO_PATH to (200 to """{"version":"${BuildConfig.BISQ_API_VERSION}","features":[]}""")),
        )
    }
    private val observer = WebSocketEventObserver()
    private val received = mutableListOf<Pair<String, Int>>()

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

    @Test
    fun `an event sent before the subscription response is delivered after the snapshot`() =
        runTest {
            node.eventsBeforeSubscriptionResponse = { subscriberId -> listOf(event(subscriberId, 1)) }
            val client = connectedClient()

            client.subscribe(Topic.TRADES, null, observer)
            testDispatcher.scheduler.advanceUntilIdle()

            val subscriberId = latestSubscriberId()
            assertEquals(listOf(subscriberId to 0, subscriberId to 1), received)
        }

    @Test
    fun `a failed subscription emits nothing`() =
        runTest {
            node.eventsBeforeSubscriptionResponse = { subscriberId -> listOf(event(subscriberId, 1)) }
            node.subscriptionError = "not authorized"
            val client = connectedClient()

            assertFailsWith<IllegalArgumentException> { client.subscribe(Topic.TRADES, null, observer) }
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(emptyList(), received)
            assertFalse(observer.hasReceivedData.first())
        }

    @Test
    fun `a subscription that times out fails and ignores a late response`() =
        runTest {
            node.answerSubscriptions = false
            val client = connectedClient()

            val result = async { runCatching { client.subscribe(Topic.TRADES, null, observer) } }
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(result.await().exceptionOrNull() is SubscribeTimeoutException)

            val subscriberId = latestSubscriberId()
            node.push(SubscriptionResponse(subscriberId, payload = "[]"))
            node.push(event(subscriberId, 1))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(emptyList(), received)
            assertFalse(observer.hasReceivedData.first())
        }

    @Test
    fun `a subscribe that cannot connect in time stays a cancellation`() =
        runTest {
            // Not connected: the reconnect resubscribes, so the wait is not a failure of the node.
            val client = client()

            val result = async { runCatching { client.subscribe(Topic.TRADES, null, observer) } }
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(result.await().exceptionOrNull() is TimeoutCancellationException)
            assertEquals(emptyList(), node.subscriptionRequests)
        }

    @Test
    fun `a subscribe cancelled by the caller's own timeout stays a cancellation`() =
        runTest {
            node.answerSubscriptions = false
            val client = connectedClient()

            val result = async { runCatching { withTimeout(1_000) { client.subscribe(Topic.TRADES, null, observer) } } }
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(result.await().exceptionOrNull() is TimeoutCancellationException)
        }

    @Test
    fun `a resubscribe ignores events still sent to the previous subscriber`() =
        runTest {
            val client = connectedClient()
            client.subscribe(Topic.TRADES, null, observer)
            testDispatcher.scheduler.advanceUntilIdle()
            val first = latestSubscriberId()

            // unSubscribe is not implemented, so the node keeps pushing to the first subscriber.
            node.eventsBeforeSubscriptionResponse = { _ -> listOf(event(first, 5)) }
            observer.resetSequence()
            client.subscribe(Topic.TRADES, null, observer)
            val second = latestSubscriberId()
            node.push(event(second, 1))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(first to 0, second to 0, second to 1), received)
        }

    private fun client() =
        WebSocketClientImpl(
            httpClient = node.httpClient,
            json = json,
            apiUrl = Url("http://localhost:8080"),
            sessionId = "session-id",
            clientId = "client-id",
            clientScope = testScope,
        )

    private suspend fun connectedClient(): WebSocketClientImpl {
        val client = client()
        assertNull(client.connect(timeout = 5_000L))
        testDispatcher.scheduler.advanceUntilIdle()
        testScope.launch { observer.webSocketEvent.collect { received += it.subscriberId to it.sequenceNumber } }
        testDispatcher.scheduler.advanceUntilIdle()
        return client
    }

    /** The subscriber id of the latest subscribe, which is also its request id. */
    private fun latestSubscriberId() = node.subscriptionRequests.last().requestId

    private fun event(
        subscriberId: String,
        sequenceNumber: Int,
    ) = WebSocketEvent(Topic.TRADES, subscriberId, "[]", ModificationType.ADDED, sequenceNumber)
}
