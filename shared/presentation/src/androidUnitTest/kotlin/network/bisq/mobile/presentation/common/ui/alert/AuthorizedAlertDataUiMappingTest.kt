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
    fun `defaultAlertHeadline returns non-empty string for INFO`() {
        assertTrue(AlertType.INFO.defaultAlertHeadline().isNotEmpty())
    }

    @Test
    fun `defaultAlertHeadline returns non-empty string for WARN`() {
        assertTrue(AlertType.WARN.defaultAlertHeadline().isNotEmpty())
    }

    @Test
    fun `defaultAlertHeadline returns non-empty string for EMERGENCY`() {
        assertTrue(AlertType.EMERGENCY.defaultAlertHeadline().isNotEmpty())
    }

    @Test
    fun `defaultAlertHeadline returns empty for BAN (else branch)`() {
        val result = AlertType.BAN.defaultAlertHeadline()
        assertTrue(result.isEmpty(), "Fallback headline for BAN must be empty")
    }
}
