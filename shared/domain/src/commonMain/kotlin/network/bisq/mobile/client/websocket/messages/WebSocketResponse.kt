package network.bisq.mobile.client.websocket.messages

interface WebSocketResponse : WebSocketMessage {
    val requestId: String
    //todo
}