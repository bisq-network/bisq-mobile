package network.bisq.mobile.presentation.tabs.my_trades.closed

import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClosedTradeListUiStateTest {
    @Test
    fun `default state has neutral defaults`() {
        val state = ClosedTradeListUiState()
        assertEquals("", state.searchQuery)
        assertEquals(TradeSort.NEWEST_FIRST, state.sortBy)
        assertEquals(TradeOutcomeFilter.ALL, state.outcomeFilter)
        assertEquals(TradeRoleFilter.ALL, state.roleFilter)
        assertFalse(state.showFilterSheet)
        assertNull(state.selectedTradeForDetails)
    }

    @Test
    fun `isFilterActive is false for default state`() {
        assertFalse(ClosedTradeListUiState().isFilterActive)
    }

    @Test
    fun `isFilterActive is true when sortBy is not the default`() {
        val state = ClosedTradeListUiState(sortBy = TradeSort.OLDEST_FIRST)
        assertTrue(state.isFilterActive)
    }

    @Test
    fun `isFilterActive is true when outcomeFilter is not ALL`() {
        val state = ClosedTradeListUiState(outcomeFilter = TradeOutcomeFilter.COMPLETED)
        assertTrue(state.isFilterActive)
    }

    @Test
    fun `isFilterActive is true when roleFilter is not ALL`() {
        val state = ClosedTradeListUiState(roleFilter = TradeRoleFilter.BUYER)
        assertTrue(state.isFilterActive)
    }

    @Test
    fun `isFilterActive ignores searchQuery`() {
        val state = ClosedTradeListUiState(searchQuery = "needle")
        assertFalse(state.isFilterActive)
    }

    @Test
    fun `isFilterActive ignores showFilterSheet`() {
        val state = ClosedTradeListUiState(showFilterSheet = true)
        assertFalse(state.isFilterActive)
    }
}
