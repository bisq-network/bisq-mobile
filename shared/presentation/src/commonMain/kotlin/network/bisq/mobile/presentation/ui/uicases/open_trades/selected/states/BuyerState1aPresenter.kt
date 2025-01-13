package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

// todo: btc address/ ln invoice validation missing
class BuyerState1aPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    private var _headline = MutableStateFlow("")
    val headline: StateFlow<String> get() = _headline

    private var _description = MutableStateFlow("")
    val description: StateFlow<String> get() = _description

    private var _bitcoinPaymentData = MutableStateFlow("")
    val bitcoinPaymentData: StateFlow<String> get() = _bitcoinPaymentData

    private var job: Job? = null

    override fun onViewAttached() {
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!
        val paymentMethod = openTradeItemModel.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
        _headline.value = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.headline.$paymentMethod".i18n()
        _description.value = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.description.$paymentMethod".i18n()
    }

    override fun onViewUnattaching() {
        job?.cancel()
        job = null
        _bitcoinPaymentData.value = ""
    }

    fun onBitcoinPaymentDataInput(value: String) {
        _bitcoinPaymentData.value = value.trim()
    }

    fun onSend() {
        require(bitcoinPaymentData.value.isNotEmpty())
        job = CoroutineScope(BackgroundDispatcher).launch {
            tradesServiceFacade.buyerSendBitcoinPaymentData(bitcoinPaymentData.value)
        }
    }

    fun onOpenWalletGuide() {
        //todo
    }
}