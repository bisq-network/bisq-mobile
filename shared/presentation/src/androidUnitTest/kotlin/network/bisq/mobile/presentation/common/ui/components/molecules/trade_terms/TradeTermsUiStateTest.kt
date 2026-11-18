package network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TradeTermsUiStateTest {
    @Test
    fun `blank text means no terms`() {
        assertNull(TradeTermsUiState.fromText(" \n\t ", TradeTermsDisplayMode.FULL))
        assertNull(TradeTermsUiState.fromText(null, TradeTermsDisplayMode.FULL))
    }

    @Test
    fun `without a cap the trimmed text is kept whole`() {
        val state = assertNotNull(TradeTermsUiState.fromText("  ${"a".repeat(5_000)}  ", TradeTermsDisplayMode.FULL))

        assertEquals("a".repeat(5_000), state.text)
        assertFalse(state.isTruncated)
    }

    @Test
    fun `text at the cap is kept whole and not flagged`() {
        val state = assertNotNull(TradeTermsUiState.fromText("a".repeat(100), TradeTermsDisplayMode.COLLAPSIBLE, maxLength = 100))

        assertEquals(100, state.text.length)
        assertFalse(state.isTruncated)
    }

    @Test
    fun `text over the cap is cut to it and flagged`() {
        val state = assertNotNull(TradeTermsUiState.fromText("a".repeat(101), TradeTermsDisplayMode.COLLAPSIBLE, maxLength = 100))

        assertEquals("a".repeat(100), state.text)
        assertTrue(state.isTruncated)
    }

    @Test
    fun `the cut never splits a surrogate pair`() {
        // 99 chars plus an emoji (two UTF-16 units) straddle a cap of 100.
        val state =
            assertNotNull(
                TradeTermsUiState.fromText("a".repeat(99) + "😀" + "tail", TradeTermsDisplayMode.COLLAPSIBLE, maxLength = 100),
            )

        assertEquals("a".repeat(99), state.text)
        assertTrue(state.isTruncated)
    }
}
