package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_3.state_b

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class BuyerStateLightning3bPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    private val _isCompleteTradeEnabled = MutableStateFlow(true)
    val isCompleteTradeEnabled: StateFlow<Boolean> = _isCompleteTradeEnabled.asStateFlow()

    fun onCompleteTrade() {
        guardedSuspendAction(
            _isCompleteTradeEnabled,
            "onCompleteTrade",
            reEnableGuardOnComplete = false,
        ) {
            val result =
                runCatching { tradesServiceFacade.btcConfirmed() }
                    .getOrElse { Result.failure(it) }
            result.onFailure {
                _isCompleteTradeEnabled.value = true
            }
        }
    }
}
