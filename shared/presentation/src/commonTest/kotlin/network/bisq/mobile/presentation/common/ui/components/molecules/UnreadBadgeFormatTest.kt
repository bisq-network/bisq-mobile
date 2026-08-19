package network.bisq.mobile.presentation.common.ui.components.molecules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the shared unread-count → badge-pill contract: hidden at zero or below,
 * exact count up to 99, capped at "99+" above.
 */
class UnreadBadgeFormatTest {
    @Test
    fun `zero and negative counts render no badge`() {
        assertNull(formatUnreadBadgeCount(0))
        assertNull(formatUnreadBadgeCount(-1))
        assertNull(formatUnreadBadgeCount(Int.MIN_VALUE))
    }

    @Test
    fun `counts up to 99 render exactly`() {
        assertEquals("1", formatUnreadBadgeCount(1))
        assertEquals("42", formatUnreadBadgeCount(42))
        assertEquals("99", formatUnreadBadgeCount(99))
    }

    @Test
    fun `counts above 99 cap at 99 plus`() {
        assertEquals("99+", formatUnreadBadgeCount(100))
        assertEquals("99+", formatUnreadBadgeCount(1234))
        assertEquals("99+", formatUnreadBadgeCount(Int.MAX_VALUE))
    }
}
