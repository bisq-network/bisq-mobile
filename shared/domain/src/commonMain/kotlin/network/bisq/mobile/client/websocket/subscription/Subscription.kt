package network.bisq.mobile.client.websocket.subscription

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.utils.Logging

class Subscription<T>(
    private val webSocketClientProvider: WebSocketClientProvider,
    private val json: Json,
    private val topic: Topic,
    private val resultHandler: (List<T>, ModificationType) -> Unit
) : Logging {

    // Misc
    private val ioScope = CoroutineScope(IODispatcher)
    private var job: Job? = null
    private var sequenceNumber = atomic(-1)

    fun subscribe() {
        require(job == null)
        job = ioScope.launch {
            // subscribe blocks until we get a response
            val observer = webSocketClientProvider.subscribe(topic)
            observer.webSocketEvent.collect { webSocketEvent ->
                try {
                    if (webSocketEvent?.deferredPayload == null) {
                        return@collect
                    }
                    if (sequenceNumber.value >= webSocketEvent.sequenceNumber) {
                        log.w {
                            "Sequence number is larger or equal than the one we " +
                                    "received from the backend. We ignore that event."
                        }
                        return@collect
                    }

                    sequenceNumber.value = webSocketEvent.sequenceNumber

                    log.d { "deferredPayload = ${webSocketEvent.deferredPayload}" }
                    val webSocketEventPayload: WebSocketEventPayload<List<T>> =
                        WebSocketEventPayload.from(json, webSocketEvent)
                    log.d { "webSocketEventPayload = $webSocketEventPayload" }

                    val payload: List<T> = webSocketEventPayload.payload
                    log.d { "payload = $payload" }
                    resultHandler(payload, webSocketEvent.modificationType)
                } catch (e: Exception) {
                    log.e { "Error at processing webSocketEvent ${e.message}" }
                    throw e
                }
            }
        }
    }

    fun dispose() {
        job?.cancel()
        job = null
    }
}