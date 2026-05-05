package network.bisq.mobile.presentation.common.ui.utils

import androidx.compose.ui.platform.UriHandler
import kotlinx.coroutines.CancellationException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class UriHandlerUtilsTest {
    @Test
    fun `openUriSafely returns true when open succeeds`() {
        val handler = CapturingUriHandler()

        val result = handler.openUriSafely("https://example.com")

        assertTrue(result)
        assertEquals(listOf("https://example.com"), handler.openedUris)
    }

    @Test
    fun `openUriSafely returns false and does not throw for regular failures`() {
        val errors = mutableListOf<Throwable>()
        val handler = ThrowingUriHandler(RuntimeException("forced"))

        val result = handler.openUriSafely("https://example.com") { throwable -> errors += throwable }

        assertFalse(result)
        assertEquals(1, errors.size)
        assertEquals("forced", errors.first().message)
    }

    @Test
    fun `openUriSafely rethrows cancellation exception`() {
        val handler = ThrowingUriHandler(CancellationException("cancelled"))

        try {
            handler.openUriSafely("https://example.com")
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    private class CapturingUriHandler : UriHandler {
        val openedUris = mutableListOf<String>()

        override fun openUri(uri: String) {
            openedUris += uri
        }
    }

    private class ThrowingUriHandler(
        private val throwable: Throwable,
    ) : UriHandler {
        override fun openUri(uri: String): Unit = throw throwable
    }
}
