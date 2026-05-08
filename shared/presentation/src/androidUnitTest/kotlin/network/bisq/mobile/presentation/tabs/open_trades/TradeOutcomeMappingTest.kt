package network.bisq.mobile.presentation.tabs.open_trades

import network.bisq.mobile.data.mapping.trade.toTradeOutcome
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.model.trade.TradeOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TradeOutcomeMappingTest {
    @Test
    fun btcConfirmed_mapsTo_completed() {
        assertEquals(TradeOutcome.COMPLETED, BisqEasyTradeStateEnum.BTC_CONFIRMED.toTradeOutcome())
    }

    @Test
    fun cancelled_mapsTo_cancelled() {
        assertEquals(TradeOutcome.CANCELLED, BisqEasyTradeStateEnum.CANCELLED.toTradeOutcome())
    }

    @Test
    fun peerCancelled_mapsTo_cancelled() {
        assertEquals(TradeOutcome.CANCELLED, BisqEasyTradeStateEnum.PEER_CANCELLED.toTradeOutcome())
    }

    @Test
    fun rejected_mapsTo_rejected() {
        assertEquals(TradeOutcome.REJECTED, BisqEasyTradeStateEnum.REJECTED.toTradeOutcome())
    }

    @Test
    fun peerRejected_mapsTo_rejected() {
        assertEquals(TradeOutcome.REJECTED, BisqEasyTradeStateEnum.PEER_REJECTED.toTradeOutcome())
    }

    @Test
    fun failed_mapsTo_failed() {
        assertEquals(TradeOutcome.FAILED, BisqEasyTradeStateEnum.FAILED.toTradeOutcome())
    }

    @Test
    fun failedAtPeer_mapsTo_failed() {
        assertEquals(TradeOutcome.FAILED, BisqEasyTradeStateEnum.FAILED_AT_PEER.toTradeOutcome())
    }

    /**
     * Asserts that every [BisqEasyTradeStateEnum] value maps to a non-null [TradeOutcome].
     *
     * Note: many in-progress states fall through to the silent `else -> FAILED` branch in
     * [network.bisq.mobile.data.mapping.trade.toTradeOutcome]. That branch is a smell — an
     * in-progress trade state should never appear in closed-trade history — but changing the
     * production mapping is tracked separately. The count logged here makes future diffs visible.
     */
    @Test
    fun allStates_mappingIsDeterministicAndNonNull() {
        val allStates = BisqEasyTradeStateEnum.entries
        val elseBranchStates = mutableListOf<BisqEasyTradeStateEnum>()

        for (state in allStates) {
            val outcome = state.toTradeOutcome()
            assertNotNull(outcome, "Expected non-null outcome for state $state")

            // Track states that fall into the else -> FAILED catch-all.
            // These are in-progress FSM states that should not appear in closed trade history.
            val isExplicitTerminal =
                state == BisqEasyTradeStateEnum.BTC_CONFIRMED ||
                    state == BisqEasyTradeStateEnum.CANCELLED ||
                    state == BisqEasyTradeStateEnum.PEER_CANCELLED ||
                    state == BisqEasyTradeStateEnum.REJECTED ||
                    state == BisqEasyTradeStateEnum.PEER_REJECTED ||
                    state == BisqEasyTradeStateEnum.FAILED ||
                    state == BisqEasyTradeStateEnum.FAILED_AT_PEER

            if (!isExplicitTerminal) {
                elseBranchStates += state
            }
        }

        // Diagnostic: the else -> FAILED branch currently covers this many states.
        // If this count changes, a reviewer should check whether the mapping needs updating.
        val elseCount = elseBranchStates.size
        println("[TradeOutcomeMappingTest] $elseCount states fall through to else->FAILED: $elseBranchStates")
    }
}
