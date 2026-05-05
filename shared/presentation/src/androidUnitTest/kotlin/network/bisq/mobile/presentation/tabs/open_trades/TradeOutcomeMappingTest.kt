package network.bisq.mobile.presentation.tabs.open_trades

import network.bisq.mobile.data.mapping.trade.toTradeOutcome
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.model.trade.TradeOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
