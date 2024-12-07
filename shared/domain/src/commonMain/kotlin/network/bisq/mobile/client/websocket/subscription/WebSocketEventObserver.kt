package network.bisq.mobile.client.websocket.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.websocket.messages.WebSocketEvent

class WebSocketEventObserver {
    private val _webSocketEvent = MutableStateFlow<WebSocketEvent?>(null)
    val webSocketEvent: StateFlow<WebSocketEvent?> = _webSocketEvent
    fun setEvent(value: WebSocketEvent) {
        _webSocketEvent.value = value
    }
}