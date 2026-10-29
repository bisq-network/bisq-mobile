package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Collector-level contract of [collectPayloads], the shared recovery path for client
 * subscriptions: a malformed event is skipped and the collector stays alive, while cancellation
 * and handler failures propagate. Decode details live in [WebSocketEventPayloadTest].
 *
 * Decoding is given a dispatcher on the test scheduler, so the hop it makes stays in virtual time.
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
                    observer.collectPayloads<Int>(json, decodeDispatcher()) { payload, event ->
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
                    observer.collectPayloads<Int>(json, decodeDispatcher()) { _, _ -> handlerCalls++ }
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
                    observer.collectPayloads<Int>(json, decodeDispatcher()) { payload, _ -> received += payload }
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
                    observer.collectPayloads<Int>(json, decodeDispatcher()) { payload, _ ->
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
                        observer.collectPayloads<Int>(json, decodeDispatcher()) { _, _ -> throw IllegalStateException("handler bug") }
                    }
                testScheduler.runCurrent()

                observer.setEvent(event("42", sequenceNumber = 1))
                testScheduler.runCurrent()

                assertTrue(collector.isCompleted, "Handler failure must end the collector")
                val error = assertFailsWith<IllegalStateException> { collector.await() }
                assertEquals("handler bug", error.message)
            }
        }

    @Test
    fun `decoding runs in the decode context and the handler in the collector's context`() =
        runTest {
            val observer = WebSocketEventObserver()
            val decodeDispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))
            val collectorDispatcher = StandardTestDispatcher(testScheduler)
            var handlerName: String? = null
            var handlerInterceptor: ContinuationInterceptor? = null
            val job =
                launch(collectorDispatcher + CoroutineName("collector")) {
                    observer.collectPayloads<Int>(json, decodeDispatcher) { _, _ ->
                        handlerName = currentCoroutineContext()[CoroutineName]?.name
                        handlerInterceptor = currentCoroutineContext()[ContinuationInterceptor]
                    }
                }
            runCurrent()

            observer.setEvent(event("42", sequenceNumber = 1))
            runCurrent()

            assertEquals(1, decodeDispatcher.dispatches, "The payload must be decoded on the decode dispatcher")
            assertEquals("collector", handlerName)
            assertEquals(collectorDispatcher, handlerInterceptor, "The handler must run back in the collector's context")
            job.cancel()
        }

    @Test
    fun `back-to-back events are delivered in order`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = mutableListOf<Int>()
            val job =
                launch {
                    observer.collectPayloads<Int>(json, decodeDispatcher()) { payload, _ -> received += payload }
                }
            runCurrent()

            // No scheduling in between: the events queue up behind the first one's decode. Deltas, since a
            // full-state REPLACE would rightly supersede the queued ones.
            observer.setEvent(event("1", sequenceNumber = 1, modificationType = ModificationType.ADDED))
            observer.setEvent(event("2", sequenceNumber = 2, modificationType = ModificationType.ADDED))
            observer.setEvent(event("3", sequenceNumber = 3, modificationType = ModificationType.ADDED))
            runCurrent()

            assertEquals(listOf(1, 2, 3), received)
            job.cancel()
        }

    @Test
    fun `a snapshot received before the collector starts is still delivered`() =
        runTest {
            val observer = WebSocketEventObserver()
            observer.setEvent(event("7", sequenceNumber = 1))
            val received = mutableListOf<Int>()

            val job =
                launch {
                    observer.collectPayloads<Int>(json, decodeDispatcher()) { payload, _ -> received += payload }
                }
            runCurrent()

            assertEquals(listOf(7), received)
            job.cancel()
        }

    @Test
    fun `cancelling while a payload is being decoded stops delivery`() =
        runTest {
            val observer = WebSocketEventObserver()
            val decodeDispatcher = HeldDispatcher()
            val received = mutableListOf<Int>()
            val job =
                launch {
                    observer.collectPayloads<Int>(json, decodeDispatcher) { payload, _ -> received += payload }
                }
            runCurrent()
            observer.setEvent(event("42", sequenceNumber = 1))
            runCurrent()
            assertEquals(1, decodeDispatcher.held, "The decode must be pending when the collector is cancelled")

            job.cancel()
            decodeDispatcher.release()
            runCurrent()

            assertTrue(job.isCancelled)
            assertEquals(emptyList(), received)
        }

    private fun TestScope.decodeDispatcher(): CoroutineDispatcher = StandardTestDispatcher(testScheduler)

    /** Counts the blocks handed to [delegate], to show that decoding ran there. */
    private class CountingDispatcher(
        private val delegate: CoroutineDispatcher,
    ) : CoroutineDispatcher() {
        var dispatches = 0
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatches++
            delegate.dispatch(context, block)
        }
    }

    /** Keeps the blocks it is given until [release], so a test can act while a decode is pending. */
    private class HeldDispatcher : CoroutineDispatcher() {
        private val blocks = mutableListOf<Runnable>()
        val held: Int get() = blocks.size

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            blocks += block
        }

        fun release() {
            val toRun = blocks.toList()
            blocks.clear()
            toRun.forEach { it.run() }
        }
    }

    private fun event(
        deferredPayload: String?,
        sequenceNumber: Int,
        modificationType: ModificationType = ModificationType.REPLACE,
    ) = WebSocketEvent(
        topic = Topic.NUM_USER_PROFILES,
        subscriberId = "test-subscriber",
        deferredPayload = deferredPayload,
        modificationType = modificationType,
        sequenceNumber = sequenceNumber,
    )
}
