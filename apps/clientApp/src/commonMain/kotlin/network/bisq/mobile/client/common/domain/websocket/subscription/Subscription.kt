package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.utils.Logging

class Subscription<T>(
    private val webSocketClientService: WebSocketClientService,
    private val json: Json,
    private val topic: Topic,
    private val resultHandler: (List<T>, ModificationType) -> Unit,
    private val parameter: String? = null,
) : Logging {
    private var job: Job? = null

    fun subscribe() {
        require(job == null)
        job =
            CoroutineScope(Dispatchers.Default).launch {
                // subscribe blocks until we get a response
                val observer = webSocketClientService.subscribe(topic, parameter)
                observer.collectPayloads<List<T>>(json) { payload, webSocketEvent ->
                    log.d { "webSocketEvent topic=$topic type=${webSocketEvent.modificationType} size=${payload.size}" }
                    try {
                        resultHandler(payload, webSocketEvent.modificationType)
                    } catch (e: Exception) {
                        log.e { "Error at processing webSocketEvent ${e.message}" }
                        throw e
                    }
                }
            }
    }

    /**
     * Cancels the collector and waits for it: the handler runs on Dispatchers.Default, so a payload it is
     * still applying would otherwise land after the owner has reset the state it feeds. The wait is not
     * cancellable so a deactivation that is itself cancelled still leaves the owner fully disposed; it is
     * short, since every suspension point in the collector answers the cancel at once.
     */
    suspend fun dispose() {
        withContext(NonCancellable) {
            job?.cancelAndJoin()
        }
        job = null
    }
}
