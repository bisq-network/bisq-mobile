package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent

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
 * Like [kotlinx.coroutines.flow.StateFlow.collect], this never returns normally.
 */
suspend fun <T> WebSocketEventObserver.collectPayloads(
    json: Json,
    handler: suspend (payload: T, event: WebSocketEvent) -> Unit,
) {
    webSocketEvent.collect { event ->
        if (event?.deferredPayload == null) {
            return@collect
        }
        val decoded = WebSocketEventPayload.from<T>(json, event) ?: return@collect
        handler(decoded.payload, event)
    }
}
