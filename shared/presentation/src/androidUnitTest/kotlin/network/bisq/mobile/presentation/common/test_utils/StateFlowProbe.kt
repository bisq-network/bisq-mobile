package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class StateFlowProbe<T> internal constructor(
    private val recordedValues: MutableList<T>,
    private val job: Job,
) {
    fun values(): List<T> = recordedValues.toList()

    fun latest(): T = recordedValues.last()

    fun mark(): Int = recordedValues.size

    fun valuesSince(mark: Int): List<T> = recordedValues.drop(mark)

    fun cancel() {
        job.cancel()
    }
}

fun <T> TestScope.probeStateFlow(stateFlow: StateFlow<T>): StateFlowProbe<T> {
    val recordedValues = mutableListOf<T>()
    val job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            stateFlow.collect { recordedValues += it }
        }

    return StateFlowProbe(recordedValues, job)
}
