package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketRestApiResponse(
    override val requestId: String,
    val statusCode: Int,
    val body: String
) : WebSocketResponse

