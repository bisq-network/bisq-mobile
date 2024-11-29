package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.Serializable

@Serializable
sealed interface WebSocketResponse : WebSocketMessage {
    val requestId: String
}