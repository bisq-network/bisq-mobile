package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.TradeReadState
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class TradeChatPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val tradeReadStateRepository: TradeReadStateRepository,
) : BasePresenter(mainPresenter), Logging {
    private var jobs: MutableSet<Job> = mutableSetOf()

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private val _chatMessages: MutableStateFlow<List<BisqEasyOpenTradeMessageModel>> = MutableStateFlow(listOf())
    val chatMessages: StateFlow<List<BisqEasyOpenTradeMessageModel>> = _chatMessages

    private val _quotedMessage: MutableStateFlow<BisqEasyOpenTradeMessageModel?> = MutableStateFlow(null)
    val quotedMessage: StateFlow<BisqEasyOpenTradeMessageModel?> = _quotedMessage

    private val _showChatRulesWarnBox: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val showChatRulesWarnBox: StateFlow<Boolean> = _showChatRulesWarnBox

    override fun onViewAttached() {
        super.onViewAttached()
        require(tradesServiceFacade.selectedTrade.value != null)
        val selectedTrade = tradesServiceFacade.selectedTrade.value!!

        jobs.add(this.presenterScope.launch {
            val settings = withContext(IODispatcher) { settingsRepository.fetch() }
            settings?.let { _showChatRulesWarnBox.value = it.showChatRulesWarnBox }
            val bisqEasyOpenTradeChannelModel = selectedTrade.bisqEasyOpenTradeChannelModel

            bisqEasyOpenTradeChannelModel.chatMessages.collect { messages ->
                _chatMessages.value = messages.toList()

                withContext(IODispatcher) {
                    val readState = tradeReadStateRepository.fetch()?.map.orEmpty().toMutableMap()
                    readState[selectedTrade.tradeId] = _chatMessages.value.size
                    tradeReadStateRepository.update(TradeReadState().apply { map = readState })
                }
            }
        })
    }

    override fun onViewUnattaching() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        super.onViewUnattaching()
    }

    fun sendChatMessage(text: String) {
        val citation = quotedMessage.value?.let { quotedMessage ->
            quotedMessage.text?.let { text ->
                CitationVO(
                    quotedMessage.senderUserProfileId,
                    text,
                    quotedMessage.id
                )
            }
        }
        jobs.add(presenterScope.launch {
            withContext(IODispatcher) {
                tradeChatMessagesServiceFacade.sendChatMessage(text, citation)
            }
            _quotedMessage.value = null
        })
    }

    fun onAddReaction(message: BisqEasyOpenTradeMessageModel, reaction: ReactionEnum) {
        jobs.add(ioScope.launch {
            tradeChatMessagesServiceFacade.addChatMessageReaction(message.id, reaction)
        })
    }

    fun onRemoveReaction(message: BisqEasyOpenTradeMessageModel, reaction: BisqEasyOpenTradeMessageReactionVO) {
        jobs.add(ioScope.launch {
            tradeChatMessagesServiceFacade.removeChatMessageReaction(message.id, reaction)
        })
    }

    fun onReply(quotedMessage: BisqEasyOpenTradeMessageModel?) {
        _quotedMessage.value = quotedMessage
    }

    fun onIgnoreUser(message: BisqEasyOpenTradeMessageModel) {
    }

    fun onReportUser(message: BisqEasyOpenTradeMessageModel) {
    }

    fun onDontShowAgainChatRulesWarningBox() {
        _showChatRulesWarnBox.value = false

        jobs.add(presenterScope.launch {
            val settings = withContext(IODispatcher) {
                settingsRepository.fetch()
            }

            settings?.let {
                it.showChatRulesWarnBox = false
                settingsRepository.update(it)
            }
        })
    }
}

