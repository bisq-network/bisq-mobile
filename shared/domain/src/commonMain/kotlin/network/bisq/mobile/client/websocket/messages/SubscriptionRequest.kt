package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.client.websocket.subscription.Topic

@Serializable
@SerialName("SubscriptionRequest")
data class SubscriptionRequest(
    override val requestId: String,
    val topic: Topic,
    val parameter: String? = null
) : WebSocketRequest

