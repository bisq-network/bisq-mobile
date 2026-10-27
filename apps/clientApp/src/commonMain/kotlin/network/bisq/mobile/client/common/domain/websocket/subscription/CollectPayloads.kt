package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Collects [WebSocketEventObserver.webSocketEvent], decodes each event's payload with the
 * serializer of its [Topic] and hands the result to [handler].
 *
 * This is the shared recovery path for client subscription collectors:
 * - an event without a payload is ignored;
 * - an event whose payload does not decode is logged by [WebSocketEventPayload.from] (topic and
 *   cause only, never the payload) and skipped, so that one malformed or version-incompatible
 *   event does not end the collector for the rest of the session;
 * - whatever [handler] throws propagates unchanged, including cancellation. Recovery is scoped to
 *   deserialization on purpose: a bug in the handler should surface, not be filed as a bad event.
 *
 * Decoding runs in [decodeContext] so that a collector on main can decode off it, and a backlog of
 * large snapshots does not stall the UI. Pass the injected default dispatcher rather than
 * [kotlinx.coroutines.Dispatchers.Default] itself, so tests keep decoding in virtual time.
 * [handler] still runs in the collector's own context. Events of one collector keep
 * their order: each is decoded and handled before the next one is taken. Across collectors that
 * order is not kept, since a large payload takes longer to decode than a small later one. A
 * collector whose handlers must apply in arrival order together with another collector's passes
 * [EmptyCoroutineContext] and decodes in place, on the lane they share (see [Subscription]); so does
 * a collector that already runs off main.
 *
 * Like [kotlinx.coroutines.flow.SharedFlow.collect], this never returns normally.
 */
suspend fun <T> WebSocketEventObserver.collectPayloads(
    json: Json,
    decodeContext: CoroutineContext,
    handler: suspend (payload: T, event: WebSocketEvent) -> Unit,
) = webSocketEvent.collectPayloads(json, decodeContext, handler)

/** Same as [WebSocketEventObserver.collectPayloads], for a flow derived from the observer's events. */
suspend fun <T> Flow<WebSocketEvent>.collectPayloads(
    json: Json,
    decodeContext: CoroutineContext,
    handler: suspend (payload: T, event: WebSocketEvent) -> Unit,
) {
    collect { event ->
        if (event.deferredPayload == null) {
            return@collect
        }
        val decoded = withContext(decodeContext) { WebSocketEventPayload.from<T>(json, event) } ?: return@collect
        handler(decoded.payload, event)
    }
}
