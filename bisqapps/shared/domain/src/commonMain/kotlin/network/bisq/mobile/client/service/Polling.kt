package network.bisq.mobile.client.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.utils.Logging

class Polling(private val intervalMillis: Long, private val task: () -> Unit) : Logging {
    private var job: Job? = null
    private var isRunning = false
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)

    fun start() {
        if (!isRunning) {
            isRunning = true
            job = coroutineScope.launch {
                while (isRunning) {
                    task()
                    delay(intervalMillis)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        cancelJob()
    }

    fun restart() {
        stop()
        start()
    }

    private fun cancelJob() {
        try {
            job?.cancel()
            job = null
        } catch (e: CancellationException) {
            log.e("Job cancel failed", e)
        }
    }
}