package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BTC_CONFIRMED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.CANCELLED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.FAILED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.FAILED_AT_PEER
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.PEER_CANCELLED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.PEER_REJECTED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.REJECTED
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class OpenTradePresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeDetailsHeaderPresenter: TradeDetailsHeaderPresenter,
    private val interruptedTradePresenter: InterruptedTradePresenter,
    explorerServiceFacade: ExplorerServiceFacade
) : BasePresenter(mainPresenter) {

    val tradeFlowPresenter = TradeFlowPresenter(mainPresenter, tradesServiceFacade, explorerServiceFacade)

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private var _tradeAbortedBoxVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val tradeAbortedBoxVisible: StateFlow<Boolean> = _tradeAbortedBoxVisible

    private var _tradeProcessBoxVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val tradeProcessBoxVisible: StateFlow<Boolean> = _tradeProcessBoxVisible

    private var _isInMediation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInMediation: StateFlow<Boolean> = _isInMediation


    override fun onViewAttached() {
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!

        presenterScope.launch {
            openTradeItemModel.bisqEasyTradeModel.tradeState.collect { tradeState ->
                tradeStateChanged(tradeState)
            }
        }

        presenterScope.launch {
            openTradeItemModel.bisqEasyOpenTradeChannelModel.isInMediation.collect { isInMediation ->
                _isInMediation.value = isInMediation
            }
        }

        presenterScope.launch {
            openTradeItemModel.bisqEasyOpenTradeChannelModel.isInMediation.collect { isInMediation ->
                _isInMediation.value = isInMediation
            }
        }
    }

    override fun onViewUnattaching() {
        _tradeAbortedBoxVisible.value = false
        _tradeProcessBoxVisible.value = false
        _isInMediation.value = false
    }

    private fun tradeStateChanged(state: BisqEasyTradeStateEnum?) {
        _tradeAbortedBoxVisible.value = false
        _tradeProcessBoxVisible.value = true

        if (state == null) {
            return
        }

        when (state) {
            BTC_CONFIRMED -> {
                //  model.getInterruptTradeButtonVisible().set(false)
                //  model.getIsTradeCompleted().set(true)
            }

            REJECTED, PEER_REJECTED -> {
                _tradeAbortedBoxVisible.value = true
                _tradeProcessBoxVisible.value = false
                /*   model.getPhaseAndInfoVisible().set(false)
                   model.getInterruptedTradeInfo().set(true)
                   model.getInterruptTradeButtonVisible().set(false)
                   applyTradeInterruptedInfo(trade, false)*/
            }

            CANCELLED, PEER_CANCELLED -> {
                _tradeAbortedBoxVisible.value = true
                _tradeProcessBoxVisible.value = false
                /* model.getPhaseAndInfoVisible().set(false)
                 model.getInterruptedTradeInfo().set(true)
                 model.getInterruptTradeButtonVisible().set(false)
                 applyTradeInterruptedInfo(trade, true)*/
            }

            FAILED -> {
                _tradeAbortedBoxVisible.value = true
                _tradeProcessBoxVisible.value = false
                /*  model.getPhaseAndInfoVisible().set(false)
                  model.getError().set(true)
                  model.getInterruptTradeButtonVisible().set(false)
                  model.getShowReportToMediatorButton().set(false)
                  model.getErrorMessage().set(
                      Res.get(
                          "bisqEasy.openTrades.failed",
                          model.getBisqEasyTrade().get().getErrorMessage()
                      )
                  )*/
            }

            FAILED_AT_PEER -> {
                _tradeAbortedBoxVisible.value = true
                _tradeProcessBoxVisible.value = false

                /* model.getPhaseAndInfoVisible().set(false)
                 model.getInterruptTradeButtonVisible().set(false)
                 model.getShowReportToMediatorButton().set(false)
                 model.getError().set(true)
                 model.getErrorMessage().set(
                     Res.get(
                         "bisqEasy.openTrades.failedAtPeer",
                         model.getBisqEasyTrade().get().getPeersErrorMessage()
                     )
                 )*/
            }

            else -> {}
        }
    }
}