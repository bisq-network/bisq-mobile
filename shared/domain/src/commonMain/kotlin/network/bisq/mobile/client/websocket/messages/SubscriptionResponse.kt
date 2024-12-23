package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SubscriptionResponse")
data class SubscriptionResponse(
    override val requestId: String,
    val payload: String? = null,
    val errorMessage: String? = null
) : WebSocketResponse

