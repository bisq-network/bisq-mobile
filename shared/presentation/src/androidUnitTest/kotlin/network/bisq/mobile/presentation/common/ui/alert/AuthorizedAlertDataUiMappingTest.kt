package network.bisq.mobile.presentation.common.ui.alert

import network.bisq.mobile.domain.model.alert.AlertType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [defaultAlertHeadline].
 *
 * The INFO / WARN / EMERGENCY branches delegate to i18n(); in a plain unit-test environment
 * the i18n key is returned as-is, so we just check non-emptiness.
 * The else branch will return empty since its not actually used
 */
class AuthorizedAlertDataUiMappingTest {
    @Test
    fun `defaultAlertHeadline returns non-empty string for message alert types`() {
        listOf(AlertType.INFO, AlertType.WARN, AlertType.EMERGENCY).forEach { type ->
            assertTrue(type.defaultAlertHeadline().isNotEmpty(), "Expected non-empty headline for $type")
        }
    }

    @Test
    fun `defaultAlertHeadline returns empty for BAN (else branch)`() {
        val result = AlertType.BAN.defaultAlertHeadline()
        assertTrue(result.isEmpty(), "Fallback headline for BAN must be empty")
    }
}
