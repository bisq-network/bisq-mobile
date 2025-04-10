package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.Serializable

@Serializable
sealed class WebSocketRequest : WebSocketMessage() {
    abstract val requestId: String
}