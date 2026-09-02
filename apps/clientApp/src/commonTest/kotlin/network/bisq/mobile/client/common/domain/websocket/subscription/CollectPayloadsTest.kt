package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Collector-level contract of [collectPayloads], the shared recovery path for client
 * subscriptions: a malformed event is skipped and the collector stays alive, while cancellation
 * and handler failures propagate. Decode details live in [WebSocketEventPayloadTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectPayloadsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `malformed event is skipped and a later valid event is delivered`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = mutableListOf<Pair<Int, Int>>()
            val job =
                launch {
                    observer.collectPayloads<Int>(json) { payload, event ->
                        received += payload to event.sequenceNumber
                    }
                }
            runCurrent()

            observer.setEvent(event("not-json", sequenceNumber = 1))
            runCurrent()
            observer.setEvent(event("42", sequenceNumber = 2))
            runCurrent()

            assertTrue(job.isActive, "Collector must survive a malformed event")
            assertEquals(listOf(42 to 2), received)
            job.cancel()
        }

    @Test
    fun `event without payload is ignored`() =
        runTest {
            val observer = WebSocketEventObserver()
            var handlerCalls = 0
            val job =
                launch {
                    observer.collectPayloads<Int>(json) { _, _ -> handlerCalls++ }
                }
            runCurrent()

            observer.setEvent(event(deferredPayload = null, sequenceNumber = 1))
            runCurrent()

            assertTrue(job.isActive)
            assertEquals(0, handlerCalls)
            job.cancel()
        }

    @Test
    fun `cancelling the collector stops delivery`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = mutableListOf<Int>()
            val job =
                launch {
                    observer.collectPayloads<Int>(json) { payload, _ -> received += payload }
                }
            runCurrent()

            job.cancel()
            runCurrent()
            observer.setEvent(event("42", sequenceNumber = 1))
            runCurrent()

            assertTrue(job.isCancelled)
            assertEquals(emptyList(), received)
        }

    @Test
    fun `cancellation thrown by the handler is not swallowed`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = mutableListOf<Int>()
            val job =
                launch {
                    observer.collectPayloads<Int>(json) { payload, _ ->
                        received += payload
                        throw CancellationException("cancelled while handling")
                    }
                }
            runCurrent()

            observer.setEvent(event("1", sequenceNumber = 1))
            runCurrent()
            observer.setEvent(event("2", sequenceNumber = 2))
            runCurrent()

            assertTrue(job.isCancelled, "CancellationException must end the collector, not be filed as a bad event")
            assertFalse(job.isActive)
            assertEquals(listOf(1), received)
        }

    @Test
    fun `exception thrown by the handler propagates`() =
        runTest {
            val observer = WebSocketEventObserver()

            supervisorScope {
                val collector =
                    async {
                        observer.collectPayloads<Int>(json) { _, _ -> throw IllegalStateException("handler bug") }
                    }
                testScheduler.runCurrent()

                observer.setEvent(event("42", sequenceNumber = 1))
                testScheduler.runCurrent()

                assertTrue(collector.isCompleted, "Handler failure must end the collector")
                val error = assertFailsWith<IllegalStateException> { collector.await() }
                assertEquals("handler bug", error.message)
            }
        }

    private fun event(
        deferredPayload: String?,
        sequenceNumber: Int,
    ) = WebSocketEvent(
        topic = Topic.NUM_USER_PROFILES,
        subscriberId = "test-subscriber",
        deferredPayload = deferredPayload,
        modificationType = ModificationType.REPLACE,
        sequenceNumber = sequenceNumber,
    )
}
