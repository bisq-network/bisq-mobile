package network.bisq.mobile.client.common.domain.websocket.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic

@Serializable
@SerialName("WebSocketEvent")
data class WebSocketEvent(
    val topic: Topic,
    val subscriberId: String,
    @SerialName("payload")
    val deferredPayload: String? = null,
    val modificationType: ModificationType,
    val sequenceNumber: Int,
) : WebSocketMessage
