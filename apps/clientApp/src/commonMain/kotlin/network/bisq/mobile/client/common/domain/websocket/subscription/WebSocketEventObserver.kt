package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.domain.utils.Logging
import kotlin.concurrent.Volatile

/**
 * The events of one subscription, for its single collector. A collector that starts late receives every
 * event since the last full state (the snapshot, or a REPLACE of a topic whose REPLACE is the whole state),
 * so it never misses the snapshot nor applies a delta on nothing, unless more than [MAX_REPLAYED_EVENTS]
 * arrived before it started: then it receives only the most recent ones.
 */
class WebSocketEventObserver : Logging {
    private class Indexed(
        val index: Long,
        val event: WebSocketEvent,
    )

    // Not a StateFlow: back-to-back events must not conflate. The replay is bounded by resetReplayCache below, not
    // by its size. The unlimited buffer keeps setEvent from suspending the reader.
    private val events = MutableSharedFlow<Indexed>(replay = Int.MAX_VALUE, extraBufferCapacity = Channel.UNLIMITED)
    private val sequenceNumber = MutableStateFlow(-1)
    private val sequenceMutex = Mutex()

    // Events before this index were replaced by a full state: a stalled collector skips them.
    private val firstValidIndex = MutableStateFlow(0L)
    private var nextIndex = 0L
    private var replayStartIndex = 0L
    private var replayedEvents = 0

    // Once a collector consumes an event of the current state, the rest reaches its buffer, so the replay only needs
    // the latest event. Volatile: the collector reads it outside the lock.
    @Volatile
    private var collectorCaughtUp = false

    private enum class SnapshotPhase { NONE, AWAITING, RECEIVED, ABANDONED }

    private var snapshotPhase = SnapshotPhase.NONE
    private var expectedSubscriberId: String? = null

    // Deltas that overtook the snapshot: the node registers the subscriber before it builds the payload.
    private val heldEvents = mutableMapOf<Int, WebSocketEvent>()

    val webSocketEvent: Flow<WebSocketEvent> =
        events
            .filter { it.index >= firstValidIndex.value }
            .onEach { if (!collectorCaughtUp) onCollected(it.index) }
            .map { it.event }

    val hasReceivedData: Flow<Boolean> = sequenceNumber.map { it != -1 }

    suspend fun resetSequence() {
        sequenceMutex.withLock { resetLocked() }
    }

    /** Only the snapshot of [subscriberId] is accepted next; events of other subscribers are dropped. */
    suspend fun awaitSnapshot(subscriberId: String) {
        sequenceMutex.withLock {
            resetLocked()
            expectedSubscriberId = subscriberId
            snapshotPhase = SnapshotPhase.AWAITING
        }
    }

    /**
     * The subscribe of [subscriberId] threw. Returns true if its snapshot was accepted anyway (the response raced
     * the timeout), so the subscription is alive. Otherwise drops what was held and every later event of it.
     */
    suspend fun abandonSnapshot(subscriberId: String): Boolean =
        sequenceMutex.withLock {
            if (expectedSubscriberId != subscriberId) return false
            if (snapshotPhase == SnapshotPhase.RECEIVED) return true
            snapshotPhase = SnapshotPhase.ABANDONED
            heldEvents.clear()
            false
        }

    suspend fun setEvent(value: WebSocketEvent) {
        sequenceMutex.withLock {
            if (expectedSubscriberId != null && value.subscriberId != expectedSubscriberId) {
                log.d { "Ignoring event of subscriber ${value.subscriberId}; expecting $expectedSubscriberId" }
                return
            }
            when (snapshotPhase) {
                SnapshotPhase.ABANDONED -> return
                SnapshotPhase.AWAITING -> {
                    if (value.sequenceNumber > 0) {
                        heldEvents[value.sequenceNumber] = value
                        return
                    }
                    snapshotPhase = SnapshotPhase.RECEIVED
                    emitLocked(value)
                    heldEvents.values.sortedBy { it.sequenceNumber }.forEach { emitLocked(it) }
                    heldEvents.clear()
                }
                SnapshotPhase.NONE, SnapshotPhase.RECEIVED -> emitLocked(value)
            }
        }
    }

    private suspend fun onCollected(index: Long) {
        // Rechecked under the lock: a newer snapshot or full state may have restarted the replay meanwhile.
        sequenceMutex.withLock { if (index >= replayStartIndex) collectorCaughtUp = true }
    }

    private fun resetLocked() {
        sequenceNumber.value = -1
        heldEvents.clear()
        clearReplayLocked(startIndex = nextIndex)
    }

    private suspend fun emitLocked(value: WebSocketEvent) {
        if (sequenceNumber.value >= value.sequenceNumber) {
            log.w {
                "Sequence number is larger or equal than the one we " +
                    "received from the backend. We ignore that event."
            }
            return
        }
        sequenceNumber.value = value.sequenceNumber
        val index = nextIndex++
        val isSnapshot = value.sequenceNumber == 0
        val replacesState =
            value.topic.replaceIsFullState && (isSnapshot || value.modificationType == ModificationType.REPLACE)
        if (isSnapshot || replacesState) {
            // The queue is skipped only where the collector replaces its whole state: other snapshots may not undo
            // a queued removal (offers keep the markets absent from theirs).
            if (replacesState) firstValidIndex.value = index
            clearReplayLocked(startIndex = index)
        } else if (collectorCaughtUp || replayedEvents >= MAX_REPLAYED_EVENTS) {
            events.resetReplayCache()
            replayedEvents = 0
        }
        replayedEvents++
        // Under the lock so events reach collectors in sequence order.
        events.emit(Indexed(index, value))
    }

    private fun clearReplayLocked(startIndex: Long) {
        events.resetReplayCache()
        replayStartIndex = startIndex
        replayedEvents = 0
        collectorCaughtUp = false
    }

    private companion object {
        // Bounds the replay while no collector has caught up.
        const val MAX_REPLAYED_EVENTS = 1_000
    }
}
