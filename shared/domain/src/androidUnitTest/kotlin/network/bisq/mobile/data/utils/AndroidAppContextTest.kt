package network.bisq.mobile.data.utils

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for AndroidAppContext — mirrors ApplicationContextProviderTest in
 * the presentation module, but additionally covers the re-init guard logic
 * (same instance → no-op, different instance → throws).
 */
class AndroidAppContextTest {
    private val mockContext = mockk<Context>()
    private val anotherMockContext = mockk<Context>()

    @After
    fun tearDown() {
        AndroidAppContext.reset()
    }

    @Test
    fun `context throws IllegalStateException when not initialized`() {
        AndroidAppContext.reset()

        val ex = assertFailsWith<IllegalStateException> { AndroidAppContext.context }
        assertTrue(
            ex.message?.contains("AndroidAppContext not initialized") == true,
            "Exception message should indicate the provider isn't initialized",
        )
    }

    @Test
    fun `initialize stores applicationContext when first called`() {
        every { mockContext.applicationContext } returns mockContext

        AndroidAppContext.initialize(mockContext)

        assertEquals(mockContext, AndroidAppContext.context)
    }

    @Test
    fun `initialize is a no-op when called again with the same instance`() {
        every { mockContext.applicationContext } returns mockContext
        AndroidAppContext.initialize(mockContext)

        // Re-init with the same context — must not throw.
        AndroidAppContext.initialize(mockContext)

        assertEquals(mockContext, AndroidAppContext.context)
    }

    @Test
    fun `initialize fails fast when called with a different Context instance`() {
        every { mockContext.applicationContext } returns mockContext
        every { anotherMockContext.applicationContext } returns anotherMockContext
        AndroidAppContext.initialize(mockContext)

        val ex =
            assertFailsWith<IllegalStateException> {
                AndroidAppContext.initialize(anotherMockContext)
            }
        assertTrue(
            ex.message?.contains("already initialized with a different Context") == true,
            "Exception message should explain the lifecycle bug; got: ${ex.message}",
        )
        // The original context is preserved — fail-fast does not swap state.
        assertEquals(mockContext, AndroidAppContext.context)
    }

    @Test
    fun `reset clears the stored context`() {
        every { mockContext.applicationContext } returns mockContext
        AndroidAppContext.initialize(mockContext)

        AndroidAppContext.reset()

        assertFailsWith<IllegalStateException> { AndroidAppContext.context }
    }
}
