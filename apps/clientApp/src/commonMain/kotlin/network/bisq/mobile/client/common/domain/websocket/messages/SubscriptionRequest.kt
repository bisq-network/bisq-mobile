package network.bisq.mobile.client.common.domain.websocket.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.domain.utils.createUuid

@Serializable
@SerialName("SubscriptionRequest")
data class SubscriptionRequest(
    val topic: Topic,
    val parameter: String? = null,
    override val requestId: String = createUuid(),
) : WebSocketRequest
