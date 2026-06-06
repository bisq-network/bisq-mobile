package network.bisq.mobile.client.common.domain.access.session

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionValidityTest {
    @Test
    fun `hasMinRemainingValidity is true when more than 15m remains`() {
        val now = 1_000_000L
        val expiresAt =
            now +
                SessionValidity.SESSION_SKIP_RECREATE_MIN_REMAINING_MS +
                SessionValidity.SESSION_EXPIRY_BUFFER_MS +
                1
        assertTrue(SessionValidity.hasMinRemainingValidity(expiresAt, now = now))
    }

    @Test
    fun `hasMinRemainingValidity is false at exact 15m threshold`() {
        val now = 1_000_000L
        val expiresAt =
            now +
                SessionValidity.SESSION_SKIP_RECREATE_MIN_REMAINING_MS +
                SessionValidity.SESSION_EXPIRY_BUFFER_MS
        assertFalse(SessionValidity.hasMinRemainingValidity(expiresAt, now = now))
    }

    @Test
    fun `hasMinRemainingValidity is false when less than 15m remains`() {
        val now = 1_000_000L
        val expiresAt =
            now +
                SessionValidity.SESSION_SKIP_RECREATE_MIN_REMAINING_MS +
                SessionValidity.SESSION_EXPIRY_BUFFER_MS -
                1
        assertFalse(SessionValidity.hasMinRemainingValidity(expiresAt, now = now))
    }

    @Test
    fun `null sessionExpiresAt fails hasMinRemainingValidity`() {
        assertFalse(SessionValidity.hasMinRemainingValidity(null, now = 0L))
    }
}
