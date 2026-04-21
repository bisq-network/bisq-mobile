package network.bisq.mobile.presentation.trade.trade_detail.states.common

import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel

data class State4UiState(
    val trade: TradeItemPresentationModel? = null,
    val showCloseTradeDialog: Boolean = false,
    val myDirectionLabel: String = "",
    val myOutcomeLabel: String = "",
)
