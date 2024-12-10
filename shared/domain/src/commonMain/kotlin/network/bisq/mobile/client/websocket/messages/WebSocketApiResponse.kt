package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketApiResponse(
    override val requestId: String,
    val statusCode: Int,
    val body: String
) : WebSocketResponse

