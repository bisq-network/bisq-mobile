package network.bisq.mobile.data.service.trades

/** A trade action targeted a trade that is not among the open trades, so nothing was sent. */
class TradeNotFoundException(
    tradeId: String,
) : Exception("Trade ${tradeId.take(8)} is not an open trade")
