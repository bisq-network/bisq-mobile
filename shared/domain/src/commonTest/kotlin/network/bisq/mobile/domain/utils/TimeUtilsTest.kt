package network.bisq.mobile.domain.utils

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeUtilsTest {
    @Test
    fun `tickerFlow emits Unit values`() =
        runTest {
            val emissions =
                TimeUtils
                    .tickerFlow(periodMillis = 10L)
                    .take(3)
                    .toList()

            assertEquals(3, emissions.size)
            emissions.forEach { assertEquals(Unit, it) }
        }

    @Test
    fun `tickerFlow with default period emits values`() =
        runTest {
            // Just verify it can be created and emits at least one value
            val emissions =
                TimeUtils
                    .tickerFlow()
                    .take(1)
                    .toList()

            assertEquals(1, emissions.size)
            assertEquals(Unit, emissions.first())
        }

    @Test
    fun `tickerFlow with custom period emits values`() =
        runTest {
            val emissions =
                TimeUtils
                    .tickerFlow(periodMillis = 50L)
                    .take(2)
                    .toList()

            assertEquals(2, emissions.size)
        }
}
