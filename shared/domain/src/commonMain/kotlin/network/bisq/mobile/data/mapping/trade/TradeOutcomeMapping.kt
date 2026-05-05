package network.bisq.mobile.data.mapping.trade

import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.model.trade.TradeOutcome

fun BisqEasyTradeStateEnum.toTradeOutcome(): TradeOutcome =
    when (this) {
        BisqEasyTradeStateEnum.BTC_CONFIRMED -> TradeOutcome.COMPLETED
        BisqEasyTradeStateEnum.CANCELLED,
        BisqEasyTradeStateEnum.PEER_CANCELLED,
        -> TradeOutcome.CANCELLED

        BisqEasyTradeStateEnum.REJECTED,
        BisqEasyTradeStateEnum.PEER_REJECTED,
        -> TradeOutcome.REJECTED

        BisqEasyTradeStateEnum.FAILED,
        BisqEasyTradeStateEnum.FAILED_AT_PEER,
        -> TradeOutcome.FAILED

        else -> TradeOutcome.FAILED
    }
