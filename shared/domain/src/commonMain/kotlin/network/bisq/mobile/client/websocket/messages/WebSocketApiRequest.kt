package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketApiRequest(
    val responseClassName: String,
    override val requestId: String,
    val method: String,
    val path: String,
    val body: String,
) : WebSocketRequest
