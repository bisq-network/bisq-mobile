package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_2.state_b

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class SellerState2bPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    private val _isConfirmFiatReceiptEnabled = MutableStateFlow(true)
    val isConfirmFiatReceiptEnabled: StateFlow<Boolean> = _isConfirmFiatReceiptEnabled.asStateFlow()

    fun onConfirmFiatReceipt() {
        guardedSuspendAction(
            _isConfirmFiatReceiptEnabled,
            "onConfirmFiatReceipt",
            reEnableGuardOnComplete = false,
        ) {
            tradesServiceFacade.sellerConfirmFiatReceipt().onFailure {
                _isConfirmFiatReceiptEnabled.value = true
            }
        }
    }
}
