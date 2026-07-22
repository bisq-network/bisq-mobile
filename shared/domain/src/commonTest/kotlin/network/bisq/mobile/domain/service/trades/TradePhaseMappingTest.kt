package network.bisq.mobile.domain.service.trades

import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade.Phase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TradePhaseMappingTest {
    @Test
    fun `maps the key payment and settlement states to the right phase`() {
        assertEquals(Phase.BUYER_2, BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION.toTradePhase(isSeller = false))
        assertEquals(Phase.SELLER_2, BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION.toTradePhase(isSeller = true))
        assertEquals(Phase.SELLER_3, BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT.toTradePhase(isSeller = true))
        assertEquals(Phase.BUYER_3, BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION.toTradePhase(isSeller = false))
    }

    @Test
    fun `role-agnostic states resolve by role`() {
        assertEquals(Phase.SELLER_1, BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST.toTradePhase(isSeller = true))
        assertEquals(Phase.BUYER_1, BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST.toTradePhase(isSeller = false))
        assertEquals(Phase.SELLER_4, BisqEasyTradeStateEnum.BTC_CONFIRMED.toTradePhase(isSeller = true))
        assertEquals(Phase.BUYER_4, BisqEasyTradeStateEnum.BTC_CONFIRMED.toTradePhase(isSeller = false))
    }

    @Test
    fun `terminal and init states carry no phase`() {
        listOf(
            BisqEasyTradeStateEnum.INIT,
            BisqEasyTradeStateEnum.REJECTED,
            BisqEasyTradeStateEnum.PEER_REJECTED,
            BisqEasyTradeStateEnum.CANCELLED,
            BisqEasyTradeStateEnum.PEER_CANCELLED,
            BisqEasyTradeStateEnum.FAILED,
            BisqEasyTradeStateEnum.FAILED_AT_PEER,
        ).forEach { state ->
            assertNull(state.toTradePhase(isSeller = true), "$state should carry no phase")
            assertNull(state.toTradePhase(isSeller = false), "$state should carry no phase")
        }
    }

    /** Drift guard: every non-terminal protocol state must map to a phase for at least one role. */
    @Test
    fun `every progress state maps to a phase`() {
        val terminal =
            setOf(
                BisqEasyTradeStateEnum.INIT,
                BisqEasyTradeStateEnum.REJECTED,
                BisqEasyTradeStateEnum.PEER_REJECTED,
                BisqEasyTradeStateEnum.CANCELLED,
                BisqEasyTradeStateEnum.PEER_CANCELLED,
                BisqEasyTradeStateEnum.FAILED,
                BisqEasyTradeStateEnum.FAILED_AT_PEER,
            )
        BisqEasyTradeStateEnum.entries.filterNot { it in terminal }.forEach { state ->
            assertNotNull(
                state.toTradePhase(isSeller = true) ?: state.toTradePhase(isSeller = false),
                "New/unmapped protocol state '$state' has no trade phase — update TradePhaseMapping",
            )
        }
    }
}
