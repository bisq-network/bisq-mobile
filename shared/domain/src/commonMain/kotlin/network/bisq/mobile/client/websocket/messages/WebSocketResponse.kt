package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.Serializable

@Serializable
sealed class WebSocketResponse : WebSocketMessage() {
    abstract val requestId: String
    //todo
}