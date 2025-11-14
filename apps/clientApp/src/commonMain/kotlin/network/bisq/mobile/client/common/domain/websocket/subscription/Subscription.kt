package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.utils.Logging

class Subscription<T>(
    private val webSocketClientService: WebSocketClientService,
    private val json: Json,
    private val topic: Topic,
    private val resultHandler: (List<T>, ModificationType) -> Unit
) : Logging {

    // Misc
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    fun subscribe() {
        require(job == null)
        job = ioScope.launch {
            // subscribe blocks until we get a response
            val observer = webSocketClientService.subscribe(topic)
            observer.webSocketEvent.collect { webSocketEvent ->
                try {
                    if (webSocketEvent?.deferredPayload == null) {
                        return@collect
                    }
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
