package network.bisq.mobile.client.websocket.subscription

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.domain.utils.getLogger

data class WebSocketEventPayload<T>(val payload: T) {
    companion object {
        inline fun <reified T> from(
            json: Json,
            webSocketEvent: WebSocketEvent
        ): WebSocketEventPayload<T> {
            val topic = webSocketEvent.topic
            val deferredPayload = webSocketEvent.deferredPayload!!
            try {
                @Suppress("UNCHECKED_CAST")
                val serializer: KSerializer<T> = serializer(topic.typeOf) as KSerializer<T>
                val payload: T = json.decodeFromString(serializer, deferredPayload)
                return WebSocketEventPayload(payload)
            } catch (e: Exception) {
                getLogger(WebSocketEventPayload::class.simpleName!!).e(
                    "Deserializing payloadJson failed. topic=$topic; payloadJson=$deferredPayload",
                    e
                )
                throw e
            }
        }
    }
}