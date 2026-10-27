package network.bisq.mobile.client.common.domain.websocket.subscription

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.redactedSummary
import kotlin.coroutines.EmptyCoroutineContext

class Subscription<T>(
    private val webSocketClientService: WebSocketClientService,
    private val json: Json,
    private val topic: Topic,
    private val resultHandler: (List<T>, ModificationType) -> Unit,
    private val parameter: String? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Logging {
    private var job: Job? = null

    fun subscribe() {
        require(job == null)
        job =
            CoroutineScope(Dispatchers.Default).launch {
                // subscribe blocks until we get a response. Not on [dispatcher]: a busy handler there would delay
                // the observer's registration, and events pushed meanwhile would be dropped.
                val observer = webSocketClientService.subscribe(topic, parameter)
                // Undispatched so the collector subscribes now, not once the lane is free: a delta meanwhile would
                // push the snapshot out of the replay. The yield then moves it onto [dispatcher], where idle collectors
                // resume in arrival order.
                launch(dispatcher, start = CoroutineStart.UNDISPATCHED) {
                    observer.webSocketEvent
                        .onSubscription { yield() }
                        // Decoded in place, on the lane: owners share it so events of their topics apply in arrival
                        // order, and decoding elsewhere would let a small later event overtake a large earlier one.
                        .collectPayloads<List<T>>(json, decodeContext = EmptyCoroutineContext) { payload, event ->
                            log.d { "webSocketEvent topic=$topic type=${event.modificationType} size=${payload.size}" }
                            try {
                                resultHandler(payload, event.modificationType)
                            } catch (e: Exception) {
                                log.e { "Error at processing webSocketEvent: ${e.redactedSummary()}" }
                                throw e
                            }
                        }
                }
            }
    }

    /**
     * Cancels the collector and waits for it: the handler runs on [dispatcher], so a payload it is
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
