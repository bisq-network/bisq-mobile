package network.bisq.mobile.client.websocket.messages

interface WebSocketRequest : WebSocketMessage {
    val requestId: String
}