package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.notification.NotificationController
import network.bisq.mobile.presentation.notification.NotificationIds
import network.bisq.mobile.presentation.ui.helpers.EMPTY_STRING
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class TradeChatPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val notificationController: NotificationController,
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade
) : BasePresenter(mainPresenter) {

    private val _selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(null)
    val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = _selectedTrade.asStateFlow()

    private val _sortedChatMessages: MutableStateFlow<List<BisqEasyOpenTradeMessageModel>> =
        MutableStateFlow(listOf())
    val sortedChatMessages: StateFlow<List<BisqEasyOpenTradeMessageModel>> get() = _sortedChatMessages.asStateFlow()

    private val _quotedMessage: MutableStateFlow<BisqEasyOpenTradeMessageModel?> =
        MutableStateFlow(null)
    val quotedMessage: StateFlow<BisqEasyOpenTradeMessageModel?> get() = _quotedMessage.asStateFlow()
    val showChatRulesWarnBox: StateFlow<Boolean> =
        settingsRepository.data.map { it.showChatRulesWarnBox }.stateIn(
            presenterScope,
            SharingStarted.Lazily, false
        )

    private val _userProfileIconByProfileId: MutableStateFlow<Map<String, PlatformImage?>> =
        MutableStateFlow(emptyMap())
    val userProfileIconByProfileId: StateFlow<Map<String, PlatformImage?>> get() = _userProfileIconByProfileId.asStateFlow()

    private val _ignoreUserId: MutableStateFlow<String> = MutableStateFlow("")
    val ignoreUserId: StateFlow<String> get() = _ignoreUserId.asStateFlow()

    private val _undoIgnoreUserId: MutableStateFlow<String> = MutableStateFlow("")
    val undoIgnoreUserId: StateFlow<String> get() = _undoIgnoreUserId.asStateFlow()

    val ignoredProfileIds: StateFlow<Set<String>> get() = userProfileServiceFacade.ignoredProfileIds

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    private val _showTradeNotFoundDialog = MutableStateFlow(false)
    val showTradeNotFoundDialog: StateFlow<Boolean> get() = _showTradeNotFoundDialog.asStateFlow()

    private val _showReportUserDialog = MutableStateFlow(false)
    val showReportUserDialog: StateFlow<Boolean> get() = _showReportUserDialog.asStateFlow()

    private val _reportUserTradeMessage = MutableStateFlow<BisqEasyOpenTradeMessageModel?>(null)
    val reportUserTradeMessage: StateFlow<BisqEasyOpenTradeMessageModel?> get() = _reportUserTradeMessage.asStateFlow()

    private val _reportUserMessage = MutableStateFlow<String?>(null)
    val reportUserMessage: StateFlow<String?> get() = _reportUserMessage.asStateFlow()

    val readCount =
        selectedTrade.combine(tradeReadStateRepository.data.map { it.map }) { trade, readStates ->
            if (trade?.tradeId != null) {
                readStates.getOrElse(trade.tradeId) { 0 }
            } else {
                -1
            }
        }.stateIn(
            scope = presenterScope,
            started = SharingStarted.Lazily,
            initialValue = -1,
        )

    private val observedChatMessages =
        MutableStateFlow<Set<BisqEasyOpenTradeMessageModel>>(emptySet())

    fun initialize(tradeId: String) {
        tradesServiceFacade.selectOpenTrade(tradeId)
        _selectedTrade.value = tradesServiceFacade.selectedTrade.value

        val currentTrade = _selectedTrade.value
        if (currentTrade == null) {
            log.w { "TradeChatPresenter.initialize called but selectedTrade is null - skipping flow collection" }
            _showTradeNotFoundDialog.value = true
            return
        }

        launchUI {
            val bisqEasyOpenTradeChannelModel = currentTrade.bisqEasyOpenTradeChannelModel
            // cancel notifications of chat related to this trade
            notificationController.cancel(NotificationIds.getNewChatMessageId(currentTrade.shortTradeId))

            collectUI(bisqEasyOpenTradeChannelModel.chatMessages) { messages ->
                observedChatMessages.update {
                    val newMessages = messages - it
                    newMessages.forEach { m ->
                        m.addMessageDeliveryStatusObserver(messageDeliveryServiceFacade)
                    }
                    messages
                }
            }

            collectUI(ignoredProfileIds.combine(bisqEasyOpenTradeChannelModel.chatMessages) { ignoredIds, messages ->
                messages.filter { message ->
                    when (message.chatMessageType) {
                        ChatMessageTypeEnum.TEXT, ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER -> !ignoredIds.contains(
                            message.senderUserProfileId
                        )

                        else -> true
                    }
                }.toList().sortedByDescending { it.date }
            }) { messages ->
                _sortedChatMessages.value = messages
                messages.forEach { message ->
                    withContext(IODispatcher) {
                        val userProfile = message.senderUserProfile
                        if (_userProfileIconByProfileId.value[userProfile.id] == null) {
                            val image = userProfileServiceFacade.getUserProfileIcon(
                                userProfile
                            )
                            _userProfileIconByProfileId.update { it + (userProfile.id to image) }
                        }
                    }
                }
            }

        }
    }

    override fun onViewUnattaching() {
        _userProfileIconByProfileId.update { emptyMap() }
        observedChatMessages.update {
            it.forEach { m -> m.removeMessageDeliveryStatusObserver(messageDeliveryServiceFacade) }
            emptySet()
        }
        super.onViewUnattaching()
    }

    fun sendChatMessage(text: String) {
        val finalText = text.trim()
        if (finalText.isEmpty()) return
        val citation = quotedMessage.value?.let { quotedMessage ->
            quotedMessage.text?.let { text ->
                CitationVO(
                    quotedMessage.senderUserProfileId, text, quotedMessage.id
                )
            }
        }
        launchIO {
            tradeChatMessagesServiceFacade.sendChatMessage(finalText, citation)
            _quotedMessage.value = null
        }
    }

    fun onResendMessage(messageId: String) {
        messageDeliveryServiceFacade.onResendMessage(messageId)
    }

    suspend fun getUserName(peerProfileId: String): String {
        return userProfileServiceFacade.findUserProfile(peerProfileId)?.userName ?: "data.na".i18n()
    }

    fun onAddReaction(message: BisqEasyOpenTradeMessageModel, reaction: ReactionEnum) {
        launchIO {
            tradeChatMessagesServiceFacade.addChatMessageReaction(message.id, reaction)
        }
    }

    fun onRemoveReaction(
        message: BisqEasyOpenTradeMessageModel,
        reaction: BisqEasyOpenTradeMessageReactionVO
    ) {
        launchIO {
            tradeChatMessagesServiceFacade.removeChatMessageReaction(message.id, reaction)
        }
    }

    fun onReply(quotedMessage: BisqEasyOpenTradeMessageModel?) {
        _quotedMessage.value = quotedMessage
    }

    fun showIgnoreUserPopup(id: String) {
        _ignoreUserId.value = id
    }

    fun hideIgnoreUserPopup() {
        _ignoreUserId.value = ""
    }

    fun showUndoIgnoreUserPopup(id: String) {
        _undoIgnoreUserId.value = id
    }

    fun hideUndoIgnoreUserPopup() {
        _undoIgnoreUserId.value = ""
    }

    fun onConfirmedIgnoreUser(id: String) {
        launchIO {
            disableInteractive()
            try {
                userProfileServiceFacade.ignoreUserProfile(id)
                hideIgnoreUserPopup()
            } catch (e: Exception) {
                log.e(e) { "Failed to ignore user $id" }
            } finally {
                enableInteractive()
            }
        }
    }

    fun onConfirmedUndoIgnoreUser(id: String) {
        launchIO {
            disableInteractive()
            try {
                userProfileServiceFacade.undoIgnoreUserProfile(id)
                hideUndoIgnoreUserPopup()
            } catch (e: Exception) {
                log.e(e) { "Failed to undo ignore user $id" }
            } finally {
                enableInteractive()
            }
        }
    }

    fun onDismissIgnoreUser() {
        this.hideIgnoreUserPopup();
    }

    fun onDismissUndoIgnoreUser() {
        this.hideUndoIgnoreUserPopup();
    }

    fun onReportUser(tradeMessage: BisqEasyOpenTradeMessageModel) {
        _reportUserTradeMessage.value = tradeMessage
        _showReportUserDialog.value = true
    }

    fun onDismissReportUserDialog() {
        _showReportUserDialog.value = false
        _reportUserMessage.value = EMPTY_STRING
    }

    fun onReportUserError(errorMessage: String, reportMessage: String) {
        _reportUserMessage.value = reportMessage
        _showReportUserDialog.value = false
        showSnackbar(
            message = errorMessage,
            isError = true,
            duration = SnackbarDuration.Short
        )
    }

    fun onOpenChatRules() {
        navigateTo(NavRoute.ChatRules)
    }

    fun onDontShowAgainChatRulesWarningBox() {
        launchUI {
            settingsRepository.setShowChatRulesWarnBox(false)
        }
    }

    fun onUpdateReadCount(newValue: Int) {
        val tradeId = selectedTrade.value?.tradeId
        if (tradeId == null) return
        launchIO {
            tradeReadStateRepository.setCount(tradeId, newValue)
        }
    }

    fun onTradeNotFoundDialogDismiss() {
        _showTradeNotFoundDialog.value = false
        navigateBack()
    }
}

