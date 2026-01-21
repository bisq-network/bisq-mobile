package network.bisq.mobile.client.common.domain.websocket.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WebSocketRestApiRequest")
data class WebSocketRestApiRequest(
    override val requestId: String,
    val method: String,
    val path: String,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
) : WebSocketRequest
