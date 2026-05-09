package network.bisq.mobile.presentation.tabs.open_trades

import network.bisq.mobile.data.mapping.trade.toTradeOutcome
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.model.trade.TradeOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
     * Asserts that every [BisqEasyTradeStateEnum] value maps to a non-null [TradeOutcome] AND
     * that every non-explicitly-terminal state lands in the silent `else -> FAILED` branch in
     * [network.bisq.mobile.data.mapping.trade.toTradeOutcome]. That branch is a smell — an
     * in-progress trade state should never appear in closed-trade history — but until the
     * production mapping is made exhaustive, this guard pins the current behavior so an
     * accidental partial change doesn't slip through.
     */
    @Test
    fun allStates_mappingIsDeterministicAndNonNull() {
        val allStates = BisqEasyTradeStateEnum.entries
        val elseBranchStates = mutableListOf<BisqEasyTradeStateEnum>()

        for (state in allStates) {
            val outcome = state.toTradeOutcome()
            assertNotNull(outcome, "Expected non-null outcome for state $state")

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

        // Regression guard: until the production mapping is made exhaustive, the silent
        // else -> FAILED branch must continue to cover every non-terminal state.
        // If any of these stop mapping to FAILED (or a new enum value gets added without
        // explicit handling and lands somewhere unexpected), this test fails.
        for (state in elseBranchStates) {
            assertEquals(
                TradeOutcome.FAILED,
                state.toTradeOutcome(),
                "Non-terminal state $state must currently map to FAILED via the else branch",
            )
        }

        // Documents the smell: there ARE non-terminal states landing in else -> FAILED today.
        // If this ever becomes false (mapping made exhaustive), this test should be revisited.
        assertTrue(
            elseBranchStates.isNotEmpty(),
            "Expected at least one non-terminal state to fall into else -> FAILED. If the " +
                "mapping has been made exhaustive, this regression guard can be removed.",
        )
    }
}
