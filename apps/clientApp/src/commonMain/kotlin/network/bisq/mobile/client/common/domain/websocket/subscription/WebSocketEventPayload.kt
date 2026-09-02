package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.domain.utils.getLogger

data class WebSocketEventPayload<T>(
    val payload: T,
) {
    companion object {
        val log = getLogger("WebSocketEventPayload")

        /**
         * Returns null when the event carries no payload, or when the payload cannot be decoded:
         * malformed JSON, a shape this build does not know (version skew with the node), or a DTO
         * that rejects the values. The caller is expected to skip such an event so that one bad
         * event does not end its collector for the rest of the session.
         *
         * A payload that decodes to JSON `null` (e.g. [Topic.TRADE_RESTRICTING_ALERT]) is a
         * success: the returned wrapper is non-null and [payload] is null.
         *
         * [CancellationException] is never swallowed. The failure log names the topic and the
         * exception class only: kotlinx quotes the JSON input in its messages, and the payload may
         * carry user data.
         */
        inline fun <reified T> from(
            json: Json,
            webSocketEvent: WebSocketEvent,
        ): WebSocketEventPayload<T>? {
            val topic = webSocketEvent.topic
            val deferredPayload = webSocketEvent.deferredPayload ?: return null
            return try {
                @Suppress("UNCHECKED_CAST")
                val serializer: KSerializer<T> = serializer(topic.typeOf) as KSerializer<T>
                val payload: T = json.decodeFromString(serializer, deferredPayload)
                WebSocketEventPayload(payload)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e { "Skipping undecodable event; topic=$topic; cause=${e::class.simpleName}" }
                null
            }
        }
    }
}
