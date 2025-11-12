package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class SellerState2bPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog = _showLoadingDialog.asStateFlow()

    override fun onViewUnattaching() {
        super.onViewUnattaching()
    }

    fun onConfirmFiatReceipt() {
        launchIO {
            _showLoadingDialog.value = true
            tradesServiceFacade.sellerConfirmFiatReceipt()
            _showLoadingDialog.value = false
        }
    }
}