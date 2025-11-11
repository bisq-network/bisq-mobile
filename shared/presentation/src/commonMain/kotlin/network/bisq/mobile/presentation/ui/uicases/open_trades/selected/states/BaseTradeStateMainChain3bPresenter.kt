package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.from
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.helpers.EMPTY_STRING
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.CONFIRMED
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.IDLE
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.IN_MEMPOOL
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.REQUEST_STARTED

// TODO: Add btc address/tx validation missing
abstract class BaseTradeStateMainChain3bPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val explorerServiceFacade: ExplorerServiceFacade
) : BasePresenter(mainPresenter) {

    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    private val _buttonText: MutableStateFlow<String> = MutableStateFlow(EMPTY_STRING)
    val buttonText: StateFlow<String> get() = _buttonText.asStateFlow()

    private val _txConfirmationState: MutableStateFlow<TxConfirmationState> = MutableStateFlow(IDLE)
    val txConfirmationState: StateFlow<TxConfirmationState> get() = _txConfirmationState.asStateFlow()

    private val _blockExplorer: MutableStateFlow<String> = MutableStateFlow(EMPTY_STRING)
    val blockExplorer: StateFlow<String> get() = _blockExplorer.asStateFlow()

    private val _balanceFromTx: MutableStateFlow<String> = MutableStateFlow(EMPTY_STRING)
    val balanceFromTx: StateFlow<String> get() = _balanceFromTx.asStateFlow()

    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage.asStateFlow()

    private val _skip: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val skip: StateFlow<Boolean> get() = _skip.asStateFlow()

    private val _amountNotMatchingDialogText: MutableStateFlow<String?> =
        MutableStateFlow(null)
    val amountNotMatchingDialogText: StateFlow<String?> get() = _amountNotMatchingDialogText.asStateFlow()

    private var txAmount: Long? = null
    private var txAmountFormatted: String? = null
    private val openTradeItemModel = tradesServiceFacade.selectedTrade.value
    private val tradeAmount = openTradeItemModel?.bisqEasyTradeModel?.contract?.baseSideAmount
    private val tradeAmountFormatted =
        if (tradeAmount != null && openTradeItemModel != null) AmountFormatter.formatAmount(
            CoinVOFactory.from(tradeAmount, openTradeItemModel.baseCurrencyCode),
            useLowPrecision = false,
            withCode = true
        ) else EMPTY_STRING

    override fun onViewAttached() {
        super.onViewAttached()
        if (txConfirmationState.value == CONFIRMED) {
            _buttonText.value = "bisqEasy.tradeState.info.phase3b.button.next".i18n()
        } else {
            _buttonText.value = "bisqEasy.tradeState.info.phase3b.button.skip".i18n()
            _skip.value = true
        }

        if (txConfirmationState.value == IDLE) {
            requestTx()
        }
    }

    override fun onViewUnattaching() {
        _buttonText.value = EMPTY_STRING
        _txConfirmationState.value = IDLE
        _blockExplorer.value = EMPTY_STRING
        _balanceFromTx.value = EMPTY_STRING
        _errorMessage.value = null
        super.onViewUnattaching()
    }

    fun onCtaClick() {
        if (txAmount != null && tradeAmount != null && txAmount != tradeAmount) {
            val errorText = getAmountNotMatchingDialogText()
            if (errorText != null) {
                _amountNotMatchingDialogText.value = errorText
                return
            }
        }
        completeTrade()
    }

    private fun getAmountNotMatchingDialogText(): String? {
        val bisqEasyTradeModel = openTradeItemModel?.bisqEasyTradeModel
        val txId: String? = bisqEasyTradeModel?.paymentProof?.value
        val address: String? = bisqEasyTradeModel?.bitcoinPaymentData?.value
        val txAmountFormatted = txAmountFormatted
        return if (address != null && txId != null && txAmountFormatted != null) {
            "bisqEasy.tradeState.info.phase3b.button.next.amountNotMatching".i18n(
                address,
                txId,
                txAmountFormatted,
                tradeAmountFormatted
            )
        } else {
            null
        }
    }

    fun onAmountNotMatchingDialogDismiss() {
        _amountNotMatchingDialogText.value = null
    }

    fun onCompleteTrade() {
        completeTrade()
    }

    private fun completeTrade() {
        launchIO {
            tradesServiceFacade.btcConfirmed()
        }
    }

    private fun requestTx() {
        if (txConfirmationState.value == REQUEST_STARTED || tradesServiceFacade.selectedTrade.value == null) {
            return
        }
        val bisqEasyTradeModel = openTradeItemModel?.bisqEasyTradeModel
        val txId: String? = bisqEasyTradeModel?.paymentProof?.value
        val address: String? = bisqEasyTradeModel?.bitcoinPaymentData?.value
        if (txId == null || address == null) {
            return
        }

        _blockExplorer.value = EMPTY_STRING
        launchUI {
            val result = withContext(Dispatchers.IO) {
                explorerServiceFacade.getSelectedBlockExplorer()
            }
            if (result.isSuccess) {
                _blockExplorer.value = result.getOrThrow()
            } else {
                _blockExplorer.value = "data.na".i18n()
            }
        }

        doRequestTx(txId, address, openTradeItemModel)
    }

    private fun doRequestTx(
        txId: String,
        address: String,
        openTradeItemModel: TradeItemPresentationModel
    ) {

        _txConfirmationState.value = REQUEST_STARTED
        _errorMessage.value = null
        _balanceFromTx.value = EMPTY_STRING
        launchUI {
            val explorerResult = withContext(Dispatchers.IO) {
                explorerServiceFacade.requestTx(txId, address)
            }

            if (explorerResult.isSuccess) {
                if (explorerResult.isConfirmed) {
                    _txConfirmationState.value = CONFIRMED
                    _buttonText.value = "bisqEasy.tradeState.info.phase3b.button.next".i18n()
                } else {
                    _txConfirmationState.value = IN_MEMPOOL

                    // We request again after 20 seconds.
                    // As its presenterScope it will get canceled when presenter is deactivated.
                    launchUI {
                        delay(20_000)
                        requestTx()
                    }
                }

                val outputValues = explorerResult.outputValues
                if (outputValues.size == 1) {
                    outputValues[0].let { transactionAmount ->
                        txAmount = transactionAmount
                        txAmountFormatted = AmountFormatter.formatAmount(
                            CoinVOFactory.from(
                                transactionAmount,
                                openTradeItemModel.baseCurrencyCode
                            ),
                            useLowPrecision = false,
                            withCode = true
                        )
                        _balanceFromTx.value = txAmountFormatted ?: EMPTY_STRING

                        if (tradeAmount != null && transactionAmount != tradeAmount) {
                            _errorMessage.value =
                                "bisqEasy.tradeState.info.phase3b.balance.invalid.amountNotMatching".i18n()
                        }
                    }
                } else if (outputValues.isEmpty()) {
                    _errorMessage.value =
                        "bisqEasy.tradeState.info.phase3b.balance.invalid.noOutputsForAddress".i18n()
                } else {
                    // Not expected use case and not further handled. User should look up in explorer to validate tx
                    _errorMessage.value =
                        "bisqEasy.tradeState.info.phase3b.balance.invalid.multipleOutputsForAddress".i18n()
                }

            } else {
                _txConfirmationState.value = TxConfirmationState.FAILED
                _errorMessage.value = "bisqEasy.tradeState.info.phase3b.txId.failed".i18n(
                    blockExplorer.value,
                    explorerResult.exceptionName!!,
                    explorerResult.errorMessage!!
                )
            }
        }
    }
}