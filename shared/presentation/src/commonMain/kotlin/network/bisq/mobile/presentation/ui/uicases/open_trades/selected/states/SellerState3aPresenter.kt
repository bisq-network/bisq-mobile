package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter


class SellerState3aPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private var _paymentProof: MutableStateFlow<String?> = MutableStateFlow(null)
    val paymentProof: StateFlow<String?> get() = _paymentProof

    private var _buttonEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val buttonEnabled: StateFlow<Boolean> get() = _buttonEnabled

    private var isLightning: Boolean = false
    private var job: Job? = null

    override fun onViewAttached() {
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!
        val paymentMethod = openTradeItemModel.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
        isLightning = paymentMethod == "LN"
        updateButtonEnableState()
    }

    override fun onViewUnattaching() {
        job?.cancel()
        job = null
        _paymentProof.value = null
    }

    fun onPaymentProofInput(value: String) {
        _paymentProof.value = value.trim().ifEmpty { null }
        updateButtonEnableState()
    }

    fun onConfirmedBtcSent() {
        if (!isLightning) {
            require(paymentProof.value != null)
        }
        job = backgroundScope.launch {
            tradesServiceFacade.sellerConfirmBtcSent(paymentProof.value)
        }
    }

    private fun updateButtonEnableState() {
        _buttonEnabled.value = isLightning || paymentProof.value != null
    }
}