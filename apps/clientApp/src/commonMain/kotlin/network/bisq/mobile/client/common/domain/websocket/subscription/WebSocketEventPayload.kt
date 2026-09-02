package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.domain.utils.getLogger
import kotlin.coroutines.cancellation.CancellationException

data class WebSocketEventPayload<T>(
    val payload: T,
) {
    companion object {
        val log = getLogger("WebSocketEventPayload")
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
