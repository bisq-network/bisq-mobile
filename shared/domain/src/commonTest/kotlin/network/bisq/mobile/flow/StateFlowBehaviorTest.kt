package network.bisq.mobile.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests to ensure StateFlows behave as expected
 */
class StateFlowBehaviorTest {

    @Test
    fun `first should block until a value is emitted indefinitely`() = runTest {
        val stateFlow = MutableStateFlow<String?>(null)
        val job = launch {
            delay(100)
            stateFlow.value = "hello"
        }
        val result = stateFlow.first { it != null }
        assertEquals("hello", result)
        job.cancel()
    }

    @Test
    fun `first should suspend forever if no value is emitted`() = runTest {
        val stateFlow = MutableStateFlow<String?>(null)
        val job = launch {
            // Will suspend forever since value never changes from null
            stateFlow.first { it != null }
        }
        delay(100)
        assertEquals(true, job.isActive)
        job.cancel()
    }
}
