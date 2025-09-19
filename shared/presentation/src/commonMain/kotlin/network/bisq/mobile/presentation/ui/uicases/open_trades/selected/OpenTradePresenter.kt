package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.ScrollState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BTC_CONFIRMED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.CANCELLED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.FAILED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.FAILED_AT_PEER
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.PEER_CANCELLED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.PEER_REJECTED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.REJECTED
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradePresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    val tradeFlowPresenter: TradeFlowPresenter,
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {

    private val _selectedTrade: MutableStateFlow<TradeItemPresentationModel?> = MutableStateFlow(null)
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = _selectedTrade.asStateFlow() // tradesServiceFacade.selectedTrade //

    private val _tradeAbortedBoxVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val tradeAbortedBoxVisible: StateFlow<Boolean> get() = _tradeAbortedBoxVisible.asStateFlow()

    private val _tradeProcessBoxVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val tradeProcessBoxVisible: StateFlow<Boolean> get() = _tradeProcessBoxVisible.asStateFlow()

    private val _isInMediation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInMediation: StateFlow<Boolean> get() = _isInMediation.asStateFlow()


    private val readCount: Flow<Int> = _selectedTrade.combine(tradeReadStateRepository.data.map { it.map }) { trade, readStates ->
        if (trade?.tradeId != null) {
            readStates.getOrElse(trade.tradeId) { 0 }
        } else {
            0
        }
    }

    private val msgCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val newMsgCount = readCount.combine(msgCount) { readCount, msgCount ->
        (msgCount - readCount).coerceAtLeast(0)
    }.stateIn(
        scope = presenterScope,
        started = SharingStarted.Lazily,
        initialValue = 0,
    )

    private val _lastChatMsg: MutableStateFlow<BisqEasyOpenTradeMessageModel?> = MutableStateFlow(null)
    val lastChatMsg: StateFlow<BisqEasyOpenTradeMessageModel?> get() = _lastChatMsg.asStateFlow()

    private val _tradePaneScrollState: MutableStateFlow<ScrollState?> = MutableStateFlow(null)

    val isUserIgnored = selectedTrade.combine(userProfileServiceFacade.ignoredProfileIds) { trade, ignoredIds ->
        trade?.peersUserProfile?.id?.let { ignoredIds.contains(it) } ?: false
    }.stateIn(
        scope = presenterScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private val _showUndoIgnoreDialog = MutableStateFlow(false)
    val showUndoIgnoreDialog: StateFlow<Boolean> get() = _showUndoIgnoreDialog.asStateFlow()

    private var _coroutineScope: CoroutineScope? = null

    private var languageJob: Job? = null
    private var tradeStateJob: Job? = null
    private var mediationJob: Job? = null

    init {
        _selectedTrade.value = tradesServiceFacade.selectedTrade.value
    }

    override fun onViewAttached() {
        super.onViewAttached()
        val selectedTrade = tradesServiceFacade.selectedTrade.value
        if (selectedTrade == null) {
            log.w { "OpenTradePresenter.onViewAttached called but selectedTrade is null - skipping initialization" }
            return
        }
        val openTradeItemModel = selectedTrade

        collectUI(openTradeItemModel.bisqEasyTradeModel.tradeState) { tradeState ->
            tradeStateChanged(tradeState)
        }

        collectUI(
            isUserIgnored.combine(openTradeItemModel.bisqEasyOpenTradeChannelModel.chatMessages) { isIgnored, messages ->
                if (isIgnored) {
                    messages.filter {
                        when (it.chatMessageType) {
                            ChatMessageTypeEnum.TEXT, ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER -> it.senderUserProfileId != openTradeItemModel.peersUserProfile.id
                            else -> true
                        }
                    }
                } else {
                    messages
                }
            }
        ) {
            msgCount.update { _ -> it.size }
            _lastChatMsg.update { _ -> it.maxByOrNull { msg -> msg.date } }
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

    fun onOpenUndoIgnoreDialog() {
        _showUndoIgnoreDialog.value = true
    }


    fun hideUndoIgnoreDialog() {
        _showUndoIgnoreDialog.value = false
    }

    fun onConfirmedUndoIgnoreUser() {
        val id = selectedTrade.value?.peersUserProfile?.id
        launchIO {
            disableInteractive()
            try {
                if (id == null) {
                    throw IllegalStateException("expected user profile id to not be null, but was null")
                }
                userProfileServiceFacade.undoIgnoreUserProfile(id)
                hideUndoIgnoreDialog()
            } catch (e: Exception) {
                log.e(e) { "Failed to undo ignore user $id" }
            } finally {
                enableInteractive()
            }
        }
    }
}
