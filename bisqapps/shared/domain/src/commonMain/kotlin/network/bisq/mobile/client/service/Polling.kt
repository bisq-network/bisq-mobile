package network.bisq.mobile.client.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.utils.Logging

class Polling(private val intervalMillis: Long, private val task: () -> Unit): Logging {
    private var job: Job? = null
    private var isRunning = false

    fun start() {
        if (!isRunning) {
            isRunning = true
            job = CoroutineScope(BackgroundDispatcher).launch {
                while (isRunning) {
                    //log.i { "poll" }
                    task()
                    delay(intervalMillis)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        job?.cancel()
        job = null
    }

    fun restart() {
        stop()
        start()
    }
}