package network.bisq.mobile.client.websocket.messages

import kotlinx.serialization.Serializable

@Serializable
sealed interface WebSocketRequest : WebSocketMessage {
    val requestId: String
}