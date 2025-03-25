package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

abstract class State4Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private val _showCloseTradeDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showCloseTradeDialog: StateFlow<Boolean> = _showCloseTradeDialog

    private var jobs: MutableSet<Job> = mutableSetOf()

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
        _showCloseTradeDialog.value = false
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    fun onCloseTrade() {
        _showCloseTradeDialog.value = true
    }

    fun onDismissCloseTrade() {
        _showCloseTradeDialog.value = false
    }

    fun onConfirmCloseTrade() {
        jobs.add(CoroutineScope(BackgroundDispatcher).launch {
            val result = tradesServiceFacade.closeTrade()
            when {
                result.isFailure -> {
                    presenterScope.launch {
                        _showCloseTradeDialog.value = false
                        _genericErrorMessage.value = result.exceptionOrNull()!!.message
                    }
                }

                result.isSuccess -> {
                    presenterScope.launch {
                        _showCloseTradeDialog.value = false
                        navigateBack()
                    }
                }
            }
        })
    }

    fun onExportTradeDate() {
        jobs.add(CoroutineScope(BackgroundDispatcher).launch {
            tradesServiceFacade.exportTradeDate()
        })
    }

    abstract fun getMyDirectionString(): String

    abstract fun getMyOutcomeString(): String
}