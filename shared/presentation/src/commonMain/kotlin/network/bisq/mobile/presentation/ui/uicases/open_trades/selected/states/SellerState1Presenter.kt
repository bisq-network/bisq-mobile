package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class SellerState1Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    private var _paymentAccountData = MutableStateFlow("")
    val paymentAccountData: StateFlow<String> get() = _paymentAccountData
    private var _paymentAccountDataValid = MutableStateFlow(false)
    val paymentAccountDataValid: StateFlow<Boolean> get() = _paymentAccountDataValid

    private var job: Job? = null

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
        job?.cancel()
        job = null
        _paymentAccountData.value = ""
    }

    fun onPaymentDataInput(value: String, isValid: Boolean) {
        _paymentAccountData.value = value.trim()
        _paymentAccountDataValid.value = isValid
    }

    fun onSendPaymentData() {
        require(paymentAccountData.value.isNotEmpty())
        job = CoroutineScope(BackgroundDispatcher).launch {
            tradesServiceFacade.sellerSendsPaymentAccount(paymentAccountData.value)
        }
    }
}