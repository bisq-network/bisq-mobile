package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.domain.utils.Logging

class WebSocketEventObserver : Logging {
    private val _webSocketEvent = MutableStateFlow<WebSocketEvent?>(null)
    val webSocketEvent: StateFlow<WebSocketEvent?> = _webSocketEvent.asStateFlow()
    private val sequenceNumber = MutableStateFlow(-1)
    private val sequenceMutex = Mutex()

    val hasReceivedData: Flow<Boolean> = sequenceNumber.map { it != -1 }

    suspend fun resetSequence() {
        sequenceMutex.withLock {
            sequenceNumber.value = -1
        }
    }

    suspend fun setEvent(value: WebSocketEvent) {
        sequenceMutex.withLock {
            if (sequenceNumber.value >= value.sequenceNumber) {
                log.w {
                    "Sequence number is larger or equal than the one we " +
                        "received from the backend. We ignore that event."
                }
                return
            }
            sequenceNumber.value = value.sequenceNumber
        }

        _webSocketEvent.value = value
    }
}
