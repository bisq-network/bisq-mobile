package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.utils.ExceptionUtils.getRootCause
import network.bisq.mobile.domain.utils.ExceptionUtils.getRootCauseMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ExceptionUtilsTest {
    @Test
    fun `getRootCause returns self when no cause`() {
        val exception = RuntimeException("test error")
        assertSame(exception, exception.getRootCause())
    }

    @Test
    fun `getRootCause returns root cause with single level nesting`() {
        val rootCause = IllegalArgumentException("root cause")
        val wrapper = RuntimeException("wrapper", rootCause)
        assertSame(rootCause, wrapper.getRootCause())
    }

    @Test
    fun `getRootCause returns root cause with multiple levels of nesting`() {
        val rootCause = IllegalStateException("deep root")
        val level2 = IllegalArgumentException("level 2", rootCause)
        val level1 = RuntimeException("level 1", level2)
        val top = Exception("top level", level1)
        assertSame(rootCause, top.getRootCause())
    }

    @Test
    fun `getRootCauseMessage returns message from root cause`() {
        val rootCause = IllegalArgumentException("root message")
        val wrapper = RuntimeException("wrapper message", rootCause)
        assertEquals("root message", wrapper.getRootCauseMessage())
    }

    @Test
    fun `getRootCauseMessage returns empty string when root cause has no message`() {
        val rootCause = IllegalArgumentException()
        val wrapper = RuntimeException("wrapper", rootCause)
        assertEquals("", wrapper.getRootCauseMessage())
    }

    @Test
    fun `getRootCauseMessage returns message when no nesting`() {
        val exception = RuntimeException("single exception")
        assertEquals("single exception", exception.getRootCauseMessage())
    }

    @Test
    fun `getRootCauseMessage returns empty string for exception without message and no cause`() {
        val exception = RuntimeException()
        assertEquals("", exception.getRootCauseMessage())
    }
}
