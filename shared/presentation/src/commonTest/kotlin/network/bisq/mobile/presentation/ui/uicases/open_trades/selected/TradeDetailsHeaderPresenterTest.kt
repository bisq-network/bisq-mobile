package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import network.bisq.mobile.domain.service.offers.MediatorNotAvailableException
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeDetailsHeaderPresenter.Companion.isMediatorError
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the mediation error handling logic improvements.
 *
 * Following the feedback to "use presenter APIs rather than duplicating logic",
 * these tests focus on the extracted helper method `isMediatorError()` that
 * centralizes the logic for identifying mediator-related errors.
 *
 * This approach:
 * 1. Tests the actual presenter API method (not duplicated logic)
 * 2. Provides a single source of truth for the error identification logic
 * 3. Makes tests maintainable - changes to logic don't break tests
 * 4. Focuses on the public interface rather than implementation details
 */
class TradeDetailsHeaderPresenterTest {

    @Test
    fun `isMediatorError should identify MediatorNotAvailableException`() {
        // Given
        val throwable = MediatorNotAvailableException("No mediator found")

        // When/Then - Testing the extracted helper logic
        assertTrue(isMediatorError(throwable))
    }

    @Test
    fun `isMediatorError should identify RuntimeException with mediator message`() {
        // Given
        val throwable = RuntimeException("No mediator found")

        // When/Then - Testing the extracted helper logic
        assertTrue(isMediatorError(throwable))
    }

    @Test
    fun `isMediatorError should handle case insensitive matching`() {
        // Given
        val throwable = RuntimeException("NO MEDIATOR AVAILABLE")

        // When/Then - Testing the extracted helper logic
        assertTrue(isMediatorError(throwable))
    }

    @Test
    fun `isMediatorError should handle partial message matching`() {
        // Given
        val throwable = RuntimeException("Error: no mediator available at this time")

        // When/Then - Testing the extracted helper logic
        assertTrue(isMediatorError(throwable))
    }

    @Test
    fun `isMediatorError should not identify other exceptions`() {
        // Given
        val throwable = RuntimeException("Network connection failed")

        // When/Then - Testing the extracted helper logic
        assertFalse(isMediatorError(throwable))
    }

    @Test
    fun `isMediatorError should handle null message gracefully`() {
        // Given
        val throwable = RuntimeException(null as String?)

        // When/Then - Testing the extracted helper logic
        assertFalse(isMediatorError(throwable))
    }
}
