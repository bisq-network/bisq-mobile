package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.websocket.subscription.Topic

@Serializable
data class SubscriptionRequest(
    val responseClassName: String,
    val webSocketEventClassName: String,
    override val requestId: String,
    val topic: Topic,
    val parameter: String? = null
) : WebSocketRequest

