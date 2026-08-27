package network.bisq.mobile.domain.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultCatchingTest {
    @Test
    fun `success is returned as Result success`() =
        runTest {
            val result = resultCatching { "ok" }
            assertEquals("ok", result.getOrNull())
        }

    @Test
    fun `generic exception becomes Result failure`() =
        runTest {
            val result = resultCatching { error("boom") }
            assertTrue(result.isFailure)
            assertEquals("boom", result.exceptionOrNull()?.message)
        }

    @Test
    fun `timeout-style cancellation with an active caller is a failure`() =
        runTest {
            val result =
                resultCatching {
                    throw CancellationException("request timed out")
                }
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is CancellationException)
        }

    @Test
    fun `genuine caller cancellation is rethrown instead of a failure`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val started = CompletableDeferred<Unit>()

            var outcome: Result<Unit>? = null
            var propagated: CancellationException? = null
            val child =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        outcome =
                            resultCatching {
                                started.complete(Unit)
                                gate.await()
                            }
                    } catch (e: CancellationException) {
                        propagated = e
                        throw e
                    }
                }

            started.await()
            child.cancel()
            child.join()

            assertNotNull(propagated)
            assertTrue(child.isCancelled)
            assertNull(outcome)
        }

    @Test
    fun `non-CE failure is rethrown when the caller is already cancelled`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val started = CompletableDeferred<Unit>()

            var outcome: Result<Unit>? = null
            var propagated: CancellationException? = null
            val child =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        outcome =
                            resultCatching {
                                started.complete(Unit)
                                try {
                                    gate.await()
                                } catch (_: CancellationException) {
                                    throw RuntimeException("teardown boom")
                                }
                            }
                    } catch (e: CancellationException) {
                        propagated = e
                        throw e
                    }
                }

            started.await()
            child.cancel()
            child.join()

            assertNotNull(propagated)
            assertTrue(child.isCancelled)
            assertNull(outcome)
        }
}
