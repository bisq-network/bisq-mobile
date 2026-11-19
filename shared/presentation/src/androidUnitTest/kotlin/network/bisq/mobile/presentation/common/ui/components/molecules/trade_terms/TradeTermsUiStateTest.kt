package network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TradeTermsUiStateTest {
    @Test
    fun `blank text means no terms`() {
        assertNull(TradeTermsUiState.fromText(" \n\t ", TradeTermsDisplayMode.FULL))
        assertNull(TradeTermsUiState.fromText(null, TradeTermsDisplayMode.FULL))
    }

    @Test
    fun `long text is trimmed and kept whole`() {
        val state = assertNotNull(TradeTermsUiState.fromText("  ${"a".repeat(10_000)}  ", TradeTermsDisplayMode.COLLAPSIBLE))

        assertEquals("a".repeat(10_000), state.text)
    }
}
