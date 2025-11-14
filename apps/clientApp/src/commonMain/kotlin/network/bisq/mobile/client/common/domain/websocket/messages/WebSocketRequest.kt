package network.bisq.mobile.client.common.domain.websocket.messages

interface WebSocketRequest : WebSocketMessage {
    val requestId: String
}