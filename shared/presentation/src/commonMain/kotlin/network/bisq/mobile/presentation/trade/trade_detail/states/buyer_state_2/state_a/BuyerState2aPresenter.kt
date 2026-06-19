package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class BuyerState2aPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    private val _isConfirmFiatSentEnabled = MutableStateFlow(true)
    val isConfirmFiatSentEnabled: StateFlow<Boolean> = _isConfirmFiatSentEnabled.asStateFlow()

    fun onConfirmFiatSent() {
        guardedSuspendAction(
            _isConfirmFiatSentEnabled,
            "onConfirmFiatSent",
            reEnableGuardOnComplete = false,
        ) {
            tradesServiceFacade.buyerConfirmFiatSent().onFailure {
                _isConfirmFiatSentEnabled.value = true
            }
        }
    }
}
