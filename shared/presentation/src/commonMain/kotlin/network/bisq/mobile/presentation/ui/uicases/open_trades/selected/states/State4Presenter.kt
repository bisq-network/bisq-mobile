package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler

abstract class State4Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private val _showCloseTradeDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showCloseTradeDialog: StateFlow<Boolean> = _showCloseTradeDialog

    override fun onViewUnattaching() {
        _showCloseTradeDialog.value = false
        super.onViewUnattaching()
    }

    fun onCloseTrade() {
        _showCloseTradeDialog.value = true
    }

    fun onDismissCloseTrade() {
        _showCloseTradeDialog.value = false
    }

    fun onConfirmCloseTrade() {
        launchUI {
            val result = withContext(IODispatcher) { tradesServiceFacade.closeTrade() }

            when {
                result.isFailure -> {
                    _showCloseTradeDialog.value = false
                    result.exceptionOrNull()?.let { exception -> GenericErrorHandler.handleGenericError(exception.message) }
                        ?: GenericErrorHandler.handleGenericError("No Exception is set in result failure")
                }

                result.isSuccess -> {
                    _showCloseTradeDialog.value = false
                    navigateBack()
                }
            }
        }
    }

    fun onExportTradeDate() {
            launchIO {
            tradesServiceFacade.exportTradeDate()
        }
    }

    abstract fun getMyDirectionString(): String

    abstract fun getMyOutcomeString(): String
}