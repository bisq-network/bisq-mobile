package network.bisq.mobile.presentation.guide.trade_guide

/** @param tradeRulesConfirmed whether the user already accepted the trade rules — the screen
 *  then labels next as plain navigation instead of an acceptance. */
data class TradeGuideTradeRulesUiState(
    val tradeRulesConfirmed: Boolean = false,
)
