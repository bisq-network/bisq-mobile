package network.bisq.mobile.client.websocket.subscription

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.domain.utils.Logging

class WebSocketEventObserver : Logging {
    private val _webSocketEvent = MutableStateFlow<WebSocketEvent?>(null)
    val webSocketEvent: StateFlow<WebSocketEvent?> get() = _webSocketEvent.asStateFlow()
    private var sequenceNumber = atomic(-1)

    fun resetSequence() {
        sequenceNumber.value = -1
    }

    fun setEvent(value: WebSocketEvent) {
        if (sequenceNumber.value >= value.sequenceNumber) {
            log.w {
                "Sequence number is larger or equal than the one we " +
                        "received from the backend. We ignore that event."
            }
            return
        }
        sequenceNumber.value = value.sequenceNumber

        _webSocketEvent.value = value
    }
}