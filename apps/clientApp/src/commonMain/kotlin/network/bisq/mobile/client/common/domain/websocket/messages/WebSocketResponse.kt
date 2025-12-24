package network.bisq.mobile.client.common.domain.websocket.messages

interface WebSocketResponse : WebSocketMessage {
    val requestId: String
    // todo
}
