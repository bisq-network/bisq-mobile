package network.bisq.mobile.domain.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

object TimeUtils {
    fun tickerFlow(periodMillis: Long = 1000L) = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(periodMillis)
        }
    }
}