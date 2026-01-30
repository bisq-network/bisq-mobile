package network.bisq.mobile.domain.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CoroutineUtilsTest {
    // awaitOrCancel tests
    @Test
    fun `awaitOrCancel returns value when value flow emits first`() =
        runTest {
            val valueFlow = flowOf("result")
            val cancelFlow = MutableSharedFlow<Unit>()

            val result = awaitOrCancel(valueFlow, cancelFlow)
            assertEquals("result", result)
        }

    @Test
    fun `awaitOrCancel throws CancellationException when cancel flow emits first`() =
        runTest {
            val valueFlow = MutableSharedFlow<String>()
            val cancelFlow = flowOf(Unit)

            assertFailsWith<CancellationException> {
                awaitOrCancel(valueFlow, cancelFlow)
            }
        }

    @Test
    fun `awaitOrCancel uses custom cancellation message`() =
        runTest {
            val valueFlow = MutableSharedFlow<String>()
            val cancelFlow = flowOf(Unit)

            val exception =
                assertFailsWith<CancellationException> {
                    awaitOrCancel(valueFlow, cancelFlow, "Custom cancel message")
                }
            assertEquals("Custom cancel message", exception.message)
        }

    @Test
    fun `awaitOrCancel uses default cancellation message`() =
        runTest {
            val valueFlow = MutableSharedFlow<String>()
            val cancelFlow = flowOf(Unit)

            val exception =
                assertFailsWith<CancellationException> {
                    awaitOrCancel(valueFlow, cancelFlow)
                }
            assertEquals("Operation cancelled", exception.message)
        }

    // awaitOrNull tests
    @Test
    fun `awaitOrNull returns value when value flow emits first`() =
        runTest {
            val valueFlow = flowOf(42)
            val cancelFlow = MutableSharedFlow<Unit>()

            val result = awaitOrNull(valueFlow, cancelFlow)
            assertEquals(42, result)
        }

    @Test
    fun `awaitOrNull returns null when cancel flow emits first`() =
        runTest {
            val valueFlow = MutableSharedFlow<Int>()
            val cancelFlow = flowOf(Unit)

            val result = awaitOrNull(valueFlow, cancelFlow)
            assertNull(result)
        }

    @Test
    fun `awaitOrNull works with different value types`() =
        runTest {
            data class TestData(
                val name: String,
                val value: Int,
            )

            val valueFlow = flowOf(TestData("test", 123))
            val cancelFlow = MutableSharedFlow<Unit>()

            val result = awaitOrNull(valueFlow, cancelFlow)
            assertEquals(TestData("test", 123), result)
        }
}
