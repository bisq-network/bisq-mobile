package network.bisq.mobile.client.common.domain.httpclient.exception

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UnauthorizedApiAccessExceptionTest {
    @Test
    fun `exception without cause has null cause`() {
        val exception = UnauthorizedApiAccessException()
        assertNull(exception.cause)
    }

    @Test
    fun `exception with cause contains the cause`() {
        val cause = RuntimeException("Original error")
        val exception = UnauthorizedApiAccessException(cause)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `exception has correct message`() {
        val exception = UnauthorizedApiAccessException()
        assertEquals("Api access was not authorized", exception.message)
    }

    @Test
    fun `exception is a RuntimeException`() {
        val exception = UnauthorizedApiAccessException()
        assertIs<RuntimeException>(exception)
    }

    @Test
    fun `exception equality works`() {
        val exception1 = UnauthorizedApiAccessException()
        val exception2 = UnauthorizedApiAccessException()
        assertEquals(exception1, exception2)
    }

    @Test
    fun `exception with same cause are equal`() {
        val cause = RuntimeException("Test")
        val exception1 = UnauthorizedApiAccessException(cause)
        val exception2 = UnauthorizedApiAccessException(cause)
        assertEquals(exception1, exception2)
    }

    @Test
    fun `exception copy works`() {
        val originalCause = RuntimeException("Original")
        val newCause = RuntimeException("New")
        val original = UnauthorizedApiAccessException(originalCause)
        val copied = original.copy(cause = newCause)
        assertEquals(newCause, copied.cause)
    }

    @Test
    fun `exception can be thrown and caught`() {
        var caught = false
        try {
            throw UnauthorizedApiAccessException()
        } catch (e: UnauthorizedApiAccessException) {
            caught = true
            assertEquals("Api access was not authorized", e.message)
        }
        assertEquals(true, caught)
    }
}
