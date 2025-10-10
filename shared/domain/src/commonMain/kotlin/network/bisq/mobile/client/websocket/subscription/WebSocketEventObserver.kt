package network.bisq.mobile.client.websocket.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.domain.utils.Logging

class WebSocketEventObserver : Logging {
    private val _webSocketEvent = MutableStateFlow<WebSocketEvent?>(null)
    val webSocketEvent: StateFlow<WebSocketEvent?> get() = _webSocketEvent.asStateFlow()
    private var sequenceNumber = -1
    private val sequenceMutex = Mutex()

    suspend fun resetSequence() {
        sequenceMutex.withLock {
            sequenceNumber = -1
        }
    }

    suspend fun setEvent(value: WebSocketEvent) {
        sequenceMutex.withLock {
            if (sequenceNumber >= value.sequenceNumber) {
                log.w {
                    "Sequence number is larger or equal than the one we " +
                            "received from the backend. We ignore that event."
                }
                return
            }
            sequenceNumber = value.sequenceNumber
        }

        _webSocketEvent.value = value
    }
}