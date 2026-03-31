package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StateFlowProbeTest {
    @Test
    fun `probe records seeded value and later emissions`() =
        runTest {
            val stateFlow = MutableStateFlow(0)

            val probe = probeStateFlow(stateFlow)

            assertEquals(listOf(0), probe.values())

            stateFlow.value = 1
            advanceUntilIdle()

            assertEquals(listOf(0, 1), probe.values())
            assertEquals(1, probe.latest())

            probe.cancel()
        }

    @Test
    fun `probe can assert that no post subscription emission happened`() =
        runTest {
            val stateFlow = MutableStateFlow("seed")

            val probe = probeStateFlow(stateFlow)
            val mark = probe.mark()

            stateFlow.value = "seed"
            advanceUntilIdle()

            assertTrue(probe.valuesSince(mark).isEmpty())

            probe.cancel()
        }

    @Test
    fun `probe can distinguish later emissions on a test by test basis`() =
        runTest {
            val stateFlow = MutableStateFlow("seed")

            val probe = probeStateFlow(stateFlow)
            val initialMark = probe.mark()

            stateFlow.value = "second"
            advanceUntilIdle()

            assertEquals(listOf("second"), probe.valuesSince(initialMark))

            val secondMark = probe.mark()
            stateFlow.value = "second"
            advanceUntilIdle()

            assertTrue(probe.valuesSince(secondMark).isEmpty())

            probe.cancel()
        }
}
