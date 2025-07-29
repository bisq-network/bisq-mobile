package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
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
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TradeChatPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter), Logging {

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private val _chatMessages: MutableStateFlow<List<BisqEasyOpenTradeMessageModel>> = MutableStateFlow(listOf())
    val chatMessages: StateFlow<List<BisqEasyOpenTradeMessageModel>> = _chatMessages

    private val _quotedMessage: MutableStateFlow<BisqEasyOpenTradeMessageModel?> = MutableStateFlow(null)
    val quotedMessage: StateFlow<BisqEasyOpenTradeMessageModel?> = _quotedMessage

    private val _showChatRulesWarnBox: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val showChatRulesWarnBox: StateFlow<Boolean> = _showChatRulesWarnBox

    private val _avatarMap: MutableStateFlow<Map<String, PlatformImage?>> = MutableStateFlow(emptyMap())
    val avatarMap: StateFlow<Map<String, PlatformImage?>> = _avatarMap

    override fun onViewAttached() {
        super.onViewAttached()
        require(tradesServiceFacade.selectedTrade.value != null)
        val selectedTrade = tradesServiceFacade.selectedTrade.value!!

        launchUI {
            val settings = withContext(IODispatcher) { settingsRepository.fetch() }
            settings?.let { _showChatRulesWarnBox.value = it.showChatRulesWarnBox }
            val bisqEasyOpenTradeChannelModel = selectedTrade.bisqEasyOpenTradeChannelModel

            bisqEasyOpenTradeChannelModel.chatMessages.collect { messages ->
                _chatMessages.value = messages.toList()

                messages.toList().forEach { message ->
                    withContext(IODispatcher) {
                        val userProfile = message.senderUserProfile
                        if (_avatarMap.value[userProfile.nym] == null) {
                            val image = userProfileServiceFacade.getUserAvatar(
                                userProfile
                            )
                            _avatarMap.update { it + (userProfile.nym to image) }
                        }
                    }
                }

                withContext(IODispatcher) {
                    val readState = tradeReadStateRepository.fetch()?.map.orEmpty().toMutableMap()
                    readState[selectedTrade.tradeId] = _chatMessages.value.size
                    tradeReadStateRepository.update(TradeReadState().apply { map = readState })
                }
            }
        }
    }

    override fun onViewUnattaching() {
        _avatarMap.update { emptyMap() }
        super.onViewUnattaching()
    }

    fun sendChatMessage(text: String, scope: CoroutineScope, scrollState: LazyListState) {
        val citation = quotedMessage.value?.let { quotedMessage ->
            quotedMessage.text?.let { text ->
                CitationVO(
                    quotedMessage.senderUserProfileId,
                    text,
                    quotedMessage.id
                )
            }
        }
        launchUI {
            withContext(IODispatcher) {
                tradeChatMessagesServiceFacade.sendChatMessage(text, citation)
            }
            scope.launch { scrollState.animateScrollToItem(Int.MAX_VALUE) }
            _quotedMessage.value = null
        }
    }

    fun onAddReaction(message: BisqEasyOpenTradeMessageModel, reaction: ReactionEnum) {
        launchIO {
            tradeChatMessagesServiceFacade.addChatMessageReaction(message.id, reaction)
        }
    }

    fun onRemoveReaction(message: BisqEasyOpenTradeMessageModel, reaction: BisqEasyOpenTradeMessageReactionVO) {
        launchIO {
            tradeChatMessagesServiceFacade.removeChatMessageReaction(message.id, reaction)
        }
    }

    fun onReply(quotedMessage: BisqEasyOpenTradeMessageModel?) {
        _quotedMessage.value = quotedMessage
    }

    fun onIgnoreUser(message: BisqEasyOpenTradeMessageModel) {
    }

    fun onReportUser(message: BisqEasyOpenTradeMessageModel) {
    }

    fun onOpenChatRules() {
        navigateTo(Routes.ChatRules)
    }

    fun onDontShowAgainChatRulesWarningBox() {
        _showChatRulesWarnBox.value = false

        launchUI {
            val settings = withContext(IODispatcher) {
                settingsRepository.fetch()
            }

            settings?.let {
                it.showChatRulesWarnBox = false
                settingsRepository.update(it)
            }
        }
    }
}

