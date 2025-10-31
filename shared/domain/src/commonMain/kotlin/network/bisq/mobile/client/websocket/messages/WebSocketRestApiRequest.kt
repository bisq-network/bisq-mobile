package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WebSocketRestApiRequest")
data class WebSocketRestApiRequest(
    override val requestId: String,
    val method: String,
    val path: String,
    val body: String,
    val authToken: String? = null,
    val authTs: String? = null,
    val authNonce: String? = null,
) : WebSocketRequest
