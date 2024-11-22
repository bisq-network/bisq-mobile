package network.bisq.mobile.client.service

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher

class Polling(private val intervalMillis: Long, private val task: () -> Unit) {
    private val log = Logger.withTag(this::class.simpleName ?: "Polling")

    private var job: Job? = null
    private var isPolling = false

    fun start() {
        if (!isPolling) {
            isPolling = true
            job = CoroutineScope(BackgroundDispatcher).launch {
                while (isPolling) {
                    //log.i { "poll" }
                    task()
                    delay(intervalMillis)
                }
            }
        }
    }

    fun stop() {
        isPolling = false
        job?.cancel()
        job = null
    }
}