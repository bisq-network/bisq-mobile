package network.bisq.mobile.presentation.trade.trade_detail.states.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.common.share.ShareFileService
import network.bisq.mobile.presentation.trade.trade_detail.export.TradeCompletedCsv
import network.bisq.mobile.presentation.trade.trade_detail.export.TradeExportCsvHeaders

abstract class State4Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val shareFileService: ShareFileService,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    private val _showCloseTradeDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showCloseTradeDialog: StateFlow<Boolean> = _showCloseTradeDialog.asStateFlow()

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
        presenterScope.launch {
            val tradeId =
                selectedTrade.value?.tradeId ?: run {
                    _showCloseTradeDialog.value = false
                    GenericErrorHandler.handleGenericError("No trade selected for closure")
                    return@launch
                }
            showLoading()
            val result = tradesServiceFacade.closeTrade()

            when {
                result.isFailure -> {
                    _showCloseTradeDialog.value = false
                    result
                        .exceptionOrNull()
                        ?.let { exception -> GenericErrorHandler.handleGenericError(exception.message) }
                        ?: GenericErrorHandler.handleGenericError("No Exception is set in result failure")
                }

                result.isSuccess -> {
                    withContext(Dispatchers.IO) {
                        tradeReadStateRepository.clearId(tradeId)
                    }
                    _showCloseTradeDialog.value = false
                    navigateBack()
                }
            }
            hideLoading()
        }
    }

    fun onExportTrade() {
        presenterScope.launch {
            val trade =
                selectedTrade.value ?: run {
                    GenericErrorHandler.handleGenericError("No trade selected for export")
                    return@launch
                }
            val headers = TradeExportCsvHeaders.resolveForTrade(trade)
            val csv =
                withContext(Dispatchers.Default) {
                    TradeCompletedCsv.buildCsv(trade, headers)
                }
            val fileName = "BisqEasy-trade-${trade.shortTradeId}.csv"
            val result = shareFileService.shareUtf8TextFile(csv, fileName)
            if (result.isFailure) {
                result.exceptionOrNull()?.let { e ->
                    GenericErrorHandler.handleGenericError(e.message)
                } ?: GenericErrorHandler.handleGenericError("Trade export failed")
            }
        }
    }

    /**
     * Temporary: builds the same UTF-8 CSV as [onExportTrade] and delivers it for clipboard copy (quick testing).
     */
    fun onCopyTradeExportCsv(onCsvReady: (String) -> Unit) {
        presenterScope.launch {
            val trade =
                selectedTrade.value ?: run {
                    GenericErrorHandler.handleGenericError("No trade selected for export")
                    return@launch
                }
            val headers = TradeExportCsvHeaders.resolveForTrade(trade)
            val csv =
                withContext(Dispatchers.Default) {
                    TradeCompletedCsv.buildCsv(trade, headers)
                }
            onCsvReady(csv)
        }
    }

    abstract fun getMyDirectionString(): String

    abstract fun getMyOutcomeString(): String
}
