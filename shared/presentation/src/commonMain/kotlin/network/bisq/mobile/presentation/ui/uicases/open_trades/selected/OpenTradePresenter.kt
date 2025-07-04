package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.ScrollState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import network.bisq.mobile.domain.data.model.TradeReadState
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BTC_CONFIRMED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.CANCELLED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.FAILED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.FAILED_AT_PEER
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.PEER_CANCELLED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.PEER_REJECTED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.REJECTED
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradePresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    val tradeFlowPresenter: TradeFlowPresenter,
    private val tradeReadStateRepository: TradeReadStateRepository,
) : BasePresenter(mainPresenter) {

    private val _selectedTrade: MutableStateFlow<TradeItemPresentationModel?> = MutableStateFlow(null)
    val selectedTrade: StateFlow<TradeItemPresentationModel?> = _selectedTrade // tradesServiceFacade.selectedTrade //

    private val _tradeAbortedBoxVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val tradeAbortedBoxVisible: StateFlow<Boolean> = _tradeAbortedBoxVisible

    private val _tradeProcessBoxVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val tradeProcessBoxVisible: StateFlow<Boolean> = _tradeProcessBoxVisible

    private val _isInMediation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInMediation: StateFlow<Boolean> = _isInMediation

    // New chat count to display over Chat FAB
    private val _newMsgCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val newMsgCount: StateFlow<Int> = _newMsgCount

    private var _tradePaneScrollState: MutableStateFlow<ScrollState?> = MutableStateFlow(null)
    private var _coroutineScope: CoroutineScope? = null

    private var languageJob: Job? = null
    private var tradeStateJob: Job? = null
    private var mediationJob: Job? = null

    init {
        _selectedTrade.value = tradesServiceFacade.selectedTrade.value
        launchUI {
            mainPresenter.languageCode
                .flatMapLatest { tradesServiceFacade.selectedTrade }
                .filterNotNull()
                .collect {
                    _selectedTrade.value = it.reformat()
                }
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!
        var initReadCount : Int? = null

        collectUI(openTradeItemModel.bisqEasyTradeModel.tradeState) { tradeState ->
            tradeStateChanged(tradeState)
        }

        collectUI(openTradeItemModel.bisqEasyOpenTradeChannelModel.chatMessages) {
            val readState = tradeReadStateRepository.fetch()?.map.orEmpty().toMutableMap()
            readState[openTradeItemModel.tradeId] = it.size
            tradeReadStateRepository.update(TradeReadState().apply { map = readState })
            if (initReadCount == null) {
                initReadCount = readState[openTradeItemModel.tradeId]
            }
            _newMsgCount.update { _ -> it.size - initReadCount!! }
        }

        collectUI(openTradeItemModel.bisqEasyOpenTradeChannelModel.isInMediation) {
            _isInMediation.value = it
        }
    }

    override fun onViewUnattaching() {
        _tradeAbortedBoxVisible.value = false
        _tradeProcessBoxVisible.value = false
        _isInMediation.value = false

        languageJob?.cancel()
        tradeStateJob?.cancel()
        mediationJob?.cancel()
        languageJob = null
        tradeStateJob = null
        mediationJob = null

        super.onViewUnattaching()
    }

    fun onOpenChat() {
        navigateTo(Routes.TradeChat)
    }

    private fun tradeStateChanged(state: BisqEasyTradeStateEnum?) {
        _tradeAbortedBoxVisible.value = false
        _tradeProcessBoxVisible.value = true

        if (state == null) {
            return
        }

        _coroutineScope?.launch {
            val scrollState = _tradePaneScrollState.value
            if (scrollState != null) {
                delay(500)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
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

    fun setTradePaneScrollState(scrollState: ScrollState) {
        _tradePaneScrollState.value = scrollState
    }

    fun setUIScope(scope: CoroutineScope) {
        _coroutineScope = scope
    }
}
