package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Delivery contract of [WebSocketEventObserver.webSocketEvent]: no event is conflated away, a late collector gets
 * everything since the last full state, a stalled collector never blocks [WebSocketEventObserver.setEvent], and events
 * that arrive before the snapshot of an awaited subscription are delivered after it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketEventObserverTest {
    @Test
    fun `consecutive events are all delivered in order`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = mutableListOf<Int>()
            val job = launch { observer.webSocketEvent.collect { received += it.sequenceNumber } }
            runCurrent()

            // E.g. paymentAccountData and tradeState of one trade, pushed back to back by the node.
            observer.setEvent(event(sequenceNumber = 1))
            observer.setEvent(event(sequenceNumber = 2))
            runCurrent()

            assertEquals(listOf(1, 2), received)
            job.cancel()
        }

    @Test
    fun `a collector that starts after the snapshot still receives it`() =
        runTest {
            val observer = WebSocketEventObserver()
            observer.setEvent(event(sequenceNumber = 0))

            val received = mutableListOf<Int>()
            val job = launch { observer.webSocketEvent.collect { received += it.sequenceNumber } }
            runCurrent()

            assertEquals(listOf(0), received)
            job.cancel()
        }

    @Test
    fun `setEvent does not wait for a stalled collector`() =
        runTest {
            val observer = WebSocketEventObserver()
            val job = launch { observer.webSocketEvent.collect { awaitCancellation() } }
            runCurrent()

            // setEvent runs on the WebSocket reader: suspending it here would stall every topic.
            withTimeout(1.seconds) {
                repeat(1_000) { observer.setEvent(event(sequenceNumber = it + 1)) }
            }
            job.cancel()
        }

    @Test
    fun `events that arrive before the snapshot are delivered after it in order`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = collectSequenceNumbers(observer)
            observer.awaitSnapshot(SUBSCRIBER_ID)

            // The node adds the subscriber before it builds the snapshot, so deltas can overtake the response.
            observer.setEvent(event(sequenceNumber = 2))
            observer.setEvent(event(sequenceNumber = 1))
            runCurrent()
            assertEquals(emptyList(), received)
            assertFalse(observer.hasReceivedData.first())

            observer.setEvent(event(sequenceNumber = 0))
            runCurrent()
            assertEquals(listOf(0, 1, 2), received)
        }

    @Test
    fun `a held event received twice is delivered once`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = collectSequenceNumbers(observer)
            observer.awaitSnapshot(SUBSCRIBER_ID)

            observer.setEvent(event(sequenceNumber = 1))
            observer.setEvent(event(sequenceNumber = 1))
            observer.setEvent(event(sequenceNumber = 0))
            runCurrent()

            assertEquals(listOf(0, 1), received)
        }

    @Test
    fun `resetSequence drops the held events`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = collectSequenceNumbers(observer)
            observer.awaitSnapshot(SUBSCRIBER_ID)
            observer.setEvent(event(sequenceNumber = 1))

            observer.resetSequence()
            observer.awaitSnapshot(SUBSCRIBER_ID)
            observer.setEvent(event(sequenceNumber = 0))
            runCurrent()

            assertEquals(listOf(0), received)
        }

    @Test
    fun `events of another subscriber are ignored`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = collectSequenceNumbers(observer)
            observer.awaitSnapshot("new")

            // The node keeps pushing to the previous subscription of a resubscribed topic.
            observer.setEvent(event(sequenceNumber = 5, subscriberId = "old"))
            observer.setEvent(event(sequenceNumber = 0, subscriberId = "new"))
            observer.setEvent(event(sequenceNumber = 1, subscriberId = "new"))
            runCurrent()

            assertEquals(listOf(0, 1), received)
        }

    @Test
    fun `abandonSnapshot after the snapshot was accepted returns true`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = collectSequenceNumbers(observer)
            observer.awaitSnapshot(SUBSCRIBER_ID)
            observer.setEvent(event(sequenceNumber = 0))

            // The response and the subscribe timeout raced; the response won.
            assertTrue(observer.abandonSnapshot(SUBSCRIBER_ID))
            observer.setEvent(event(sequenceNumber = 1))
            runCurrent()

            assertEquals(listOf(0, 1), received)
        }

    @Test
    fun `a snapshot after abandonSnapshot is dropped`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = collectSequenceNumbers(observer)
            observer.awaitSnapshot(SUBSCRIBER_ID)
            observer.setEvent(event(sequenceNumber = 1))

            assertFalse(observer.abandonSnapshot(SUBSCRIBER_ID))
            observer.setEvent(event(sequenceNumber = 0))
            observer.setEvent(event(sequenceNumber = 2))
            runCurrent()

            assertEquals(emptyList(), received)
            assertFalse(observer.hasReceivedData.first())
        }

    @Test
    fun `abandonSnapshot of a superseded subscriber changes nothing`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = collectSequenceNumbers(observer)
            observer.awaitSnapshot("a")
            observer.awaitSnapshot("b")

            assertFalse(observer.abandonSnapshot("a"))
            observer.setEvent(event(sequenceNumber = 0, subscriberId = "b"))
            runCurrent()

            assertEquals(listOf(0), received)
        }

    @Test
    fun `a collector handler that takes the observer lock does not block setEvent`() =
        runTest {
            val observer = WebSocketEventObserver()
            // Unconfined: the handler runs inside setEvent's emit, like a facade that resets on a snapshot.
            val job = launch(Dispatchers.Unconfined) { observer.webSocketEvent.collect { observer.resetSequence() } }

            withTimeout(1.seconds) {
                observer.setEvent(event(sequenceNumber = 0))
                observer.setEvent(event(sequenceNumber = 1))
            }
            job.cancel()
        }

    @Test
    fun `a collector that starts after a delta still receives the snapshot first`() =
        runTest {
            val observer = WebSocketEventObserver()
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE))
            observer.setEvent(event(sequenceNumber = 1))

            val received = collectSequenceNumbers(observer)

            assertEquals(listOf(0, 1), received)
        }

    @Test
    fun `a full-state replace replaces what late collectors receive`() =
        runTest {
            val observer = WebSocketEventObserver()
            observer.setEvent(fullStateEvent(sequenceNumber = 0))
            observer.setEvent(fullStateEvent(sequenceNumber = 1))
            observer.setEvent(event(sequenceNumber = 2, topic = Topic.MARKET_PRICE))

            val received = collectSequenceNumbers(observer)

            assertEquals(listOf(1, 2), received)
        }

    @Test
    fun `a stalled collector skips events superseded by a newer full state`() =
        runTest {
            val observer = WebSocketEventObserver()
            val gate = CompletableDeferred<Unit>()
            val received = collectSequenceNumbers(observer) { if (it == 0) gate.await() }
            observer.setEvent(fullStateEvent(sequenceNumber = 0))
            runCurrent()
            observer.setEvent(event(sequenceNumber = 1, topic = Topic.MARKET_PRICE))
            observer.setEvent(fullStateEvent(sequenceNumber = 2))

            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf(0, 2), received)
        }

    @Test
    fun `a trade properties replace does not drop earlier events`() =
        runTest {
            val observer = WebSocketEventObserver()
            val gate = CompletableDeferred<Unit>()
            val received = collectSequenceNumbers(observer) { if (it == 0) gate.await() }
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE))
            runCurrent()
            observer.setEvent(event(sequenceNumber = 1))
            // One trade per event, so a REPLACE here is not the whole state.
            observer.setEvent(event(sequenceNumber = 2, modificationType = ModificationType.REPLACE))

            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf(0, 1, 2), received)
        }

    @Test
    fun `a collector that attaches after resetSequence gets nothing from the previous session`() =
        runTest {
            val observer = WebSocketEventObserver()
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE))
            observer.setEvent(event(sequenceNumber = 1))
            observer.resetSequence()

            val received = collectSequenceNumbers(observer)

            assertEquals(emptyList(), received)
        }

    @Test
    fun `after a collector subscribes past the snapshot the replay keeps only the latest event`() =
        runTest {
            val observer = WebSocketEventObserver()
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE, topic = Topic.OFFERS))
            collectSequenceNumbers(observer)
            (1..5).forEach { observer.setEvent(event(sequenceNumber = it, topic = Topic.OFFERS)) }

            val late = collectSequenceNumbers(observer)

            assertEquals(listOf(5), late)
        }

    @Test
    fun `a collector still attached from before the snapshot does not cut the replay`() =
        runTest {
            val observer = WebSocketEventObserver()
            // E.g. a collector being cancelled without join when its facade re-activates.
            collectSequenceNumbers(observer)
            observer.awaitSnapshot(SUBSCRIBER_ID)
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE))
            observer.setEvent(event(sequenceNumber = 1))
            observer.setEvent(event(sequenceNumber = 2))

            val late = collectSequenceNumbers(observer)

            assertEquals(listOf(0, 1, 2), late)
        }

    @Test
    fun `a collector attached before the snapshot keeps the replay to the latest event once it has the snapshot`() =
        runTest {
            val observer = WebSocketEventObserver()
            // E.g. a facade that starts collecting before the connection is up.
            collectSequenceNumbers(observer)
            observer.awaitSnapshot(SUBSCRIBER_ID)
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE, topic = Topic.OFFERS))
            runCurrent()
            (1..5).forEach { observer.setEvent(event(sequenceNumber = it, topic = Topic.OFFERS)) }

            val late = collectSequenceNumbers(observer)

            assertEquals(listOf(5), late)
        }

    @Test
    fun `resetSequence keeps ignoring events of other subscribers`() =
        runTest {
            val observer = WebSocketEventObserver()
            val received = collectSequenceNumbers(observer)
            observer.awaitSnapshot(SUBSCRIBER_ID)
            observer.setEvent(event(sequenceNumber = 0))
            runCurrent()

            // E.g. a frame of a superseded subscriber the listener read before the disconnect.
            observer.resetSequence()
            observer.setEvent(event(sequenceNumber = 1, subscriberId = "old"))
            runCurrent()

            assertEquals(listOf(0), received)
        }

    @Test
    fun `a collector that attaches after the cap cut the snapshot keeps the replay to the latest event`() =
        runTest {
            val observer = WebSocketEventObserver()
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE, topic = Topic.OFFERS))
            (1..1_001).forEach { observer.setEvent(event(sequenceNumber = it, topic = Topic.OFFERS)) }
            collectSequenceNumbers(observer)
            (1_002..1_006).forEach { observer.setEvent(event(sequenceNumber = it, topic = Topic.OFFERS)) }

            val late = collectSequenceNumbers(observer)

            assertEquals(listOf(1_006), late)
        }

    @Test
    fun `the replay is capped`() =
        runTest {
            val observer = WebSocketEventObserver()
            collectSequenceNumbers(observer)
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE, topic = Topic.OFFERS))
            (1..1_001).forEach { observer.setEvent(event(sequenceNumber = it, topic = Topic.OFFERS)) }

            val late = collectSequenceNumbers(observer)

            assertTrue(late.size <= 1_000, "replayed ${late.size}")
        }

    @Test
    fun `a stalled collector still receives its queued events when a resubscribe snapshot follows`() =
        runTest {
            val observer = WebSocketEventObserver()
            val gate = CompletableDeferred<Unit>()
            val received = collectSequenceNumbers(observer) { if (it == 0) gate.await() }
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE, topic = Topic.OFFERS))
            runCurrent()
            // The offers snapshot replaces only the markets it holds, so this removal must not be skipped.
            observer.setEvent(event(sequenceNumber = 1, modificationType = ModificationType.REMOVED, topic = Topic.OFFERS))

            observer.resetSequence()
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE, topic = Topic.OFFERS))
            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf(0, 1, 0), received)
        }

    @Test
    fun `a queued event from before a resubscribe snapshot does not cut that snapshot from the replay`() =
        runTest {
            val observer = WebSocketEventObserver()
            val gate = CompletableDeferred<Unit>()
            collectSequenceNumbers(observer) { if (it == 0) gate.await() else awaitCancellation() }
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE))
            runCurrent()
            observer.setEvent(event(sequenceNumber = 1))
            observer.resetSequence()
            observer.setEvent(event(sequenceNumber = 0, modificationType = ModificationType.REPLACE))
            gate.complete(Unit)
            runCurrent()
            observer.setEvent(event(sequenceNumber = 1))

            val late = collectSequenceNumbers(observer)

            assertEquals(listOf(0, 1), late)
        }

    @Test
    fun `a stalled collector of a full-state topic skips its queue when a resubscribe snapshot follows`() =
        runTest {
            val observer = WebSocketEventObserver()
            val gate = CompletableDeferred<Unit>()
            val received = collectSequenceNumbers(observer) { if (it == 0) gate.await() }
            observer.setEvent(fullStateEvent(sequenceNumber = 0))
            runCurrent()
            observer.setEvent(event(sequenceNumber = 1, topic = Topic.MARKET_PRICE))

            observer.resetSequence()
            observer.setEvent(fullStateEvent(sequenceNumber = 0))
            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf(0, 0), received)
        }

    /** Starts a collector that records sequence numbers, then runs [handler]; the caller drives it with `runCurrent()`. */
    private fun TestScope.collectSequenceNumbers(
        observer: WebSocketEventObserver,
        handler: suspend (Int) -> Unit = {},
    ): List<Int> {
        val received = mutableListOf<Int>()
        backgroundScope.launch {
            observer.webSocketEvent.collect {
                received += it.sequenceNumber
                handler(it.sequenceNumber)
            }
        }
        runCurrent()
        return received
    }

    private fun event(
        sequenceNumber: Int,
        subscriberId: String = SUBSCRIBER_ID,
        modificationType: ModificationType = ModificationType.ADDED,
        topic: Topic = Topic.TRADE_PROPERTIES,
    ) = WebSocketEvent(
        topic = topic,
        subscriberId = subscriberId,
        deferredPayload = "[]",
        modificationType = modificationType,
        sequenceNumber = sequenceNumber,
    )

    /** MARKET_PRICE sends its whole state on every REPLACE. */
    private fun fullStateEvent(sequenceNumber: Int) = event(sequenceNumber, modificationType = ModificationType.REPLACE, topic = Topic.MARKET_PRICE)

    private companion object {
        const val SUBSCRIBER_ID = "test-subscriber"
    }
}
