package network.bisq.mobile.domain.utils

import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum

/**
 * A trade parked in [BisqEasyTradeStateEnum.INIT] beyond the threshold is desynced from the peer:
 * the bisq2 trade FSM queues out-of-order protocol messages in memory only, so a restart strands
 * the trade in INIT where no incoming message can move it forward and the trade screen renders no
 * actionable step. The peer's side of the trade is typically healthy. A normal take-offer
 * handshake completes within seconds even over Tor, so past the threshold this is the stuck-FSM
 * bug, not latency.
 */
object TradeOutOfSyncDetector {
    const val OUT_OF_SYNC_THRESHOLD_MS: Long = 10 * 60 * 1000L

    fun isOutOfSync(
        trade: BisqEasyTradeModel,
        nowMs: Long = DateUtils.now(),
    ): Boolean =
        trade.tradeState.value == BisqEasyTradeStateEnum.INIT &&
            nowMs - trade.takeOfferDate > OUT_OF_SYNC_THRESHOLD_MS
}
