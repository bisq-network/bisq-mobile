package network.bisq.mobile.presentation.main

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for ApplicationContextProvider.
 * These tests verify the singleton context provider behavior.
 */
class ApplicationContextProviderTest {
    private val mockContext = mockk<Context>()

    @After
    fun tearDown() {
        ApplicationContextProvider.reset()
    }

    @Test
    fun `when context not initialized then throws IllegalStateException`() {
        // Given - context is not initialized (reset in tearDown ensures clean state)
        ApplicationContextProvider.reset()

        // When/Then
        val exception =
            assertFailsWith<IllegalStateException> {
                ApplicationContextProvider.context
            }
        assertTrue(
            exception.message?.contains("ApplicationContextProvider not initialized") == true,
            "Exception message should indicate provider not initialized",
        )
    }

    @Test
    fun `when context initialized then returns context`() {
        // Given
        every { mockContext.applicationContext } returns mockContext
        ApplicationContextProvider.initialize(mockContext)

        // When
        val result = ApplicationContextProvider.context

        // Then
        assertEquals(mockContext, result)
    }

    @Test
    fun `when reset called then context becomes unavailable`() {
        // Given
        every { mockContext.applicationContext } returns mockContext
        ApplicationContextProvider.initialize(mockContext)
        assertEquals(mockContext, ApplicationContextProvider.context)

        // When
        ApplicationContextProvider.reset()

        // Then
        assertFailsWith<IllegalStateException> {
            ApplicationContextProvider.context
        }
    }
}
