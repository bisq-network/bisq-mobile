package network.bisq.mobile.presentation.trade.trade_chat

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.deriveMentionCandidates
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.trades.selectOpenTradeWhenSynced
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationIds
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

class TradeChatPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val notificationController: NotificationController,
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(TradeChatUiState())
    val uiState: StateFlow<TradeChatUiState> = _uiState.asStateFlow()

    private val _userProfileIconByProfileId: MutableStateFlow<Map<String, PlatformImage?>> =
        MutableStateFlow(emptyMap())
    val userProfileIconByProfileId: StateFlow<Map<String, PlatformImage?>> = _userProfileIconByProfileId.asStateFlow()

    private val _isSendChatMessageEnabled = MutableStateFlow(true)
    val isSendChatMessageEnabled: StateFlow<Boolean> = _isSendChatMessageEnabled.asStateFlow()

    private val _isConfirmIgnoreUserEnabled = MutableStateFlow(true)
    val isConfirmIgnoreUserEnabled: StateFlow<Boolean> = _isConfirmIgnoreUserEnabled.asStateFlow()

    private val _isConfirmUndoIgnoreUserEnabled = MutableStateFlow(true)
    val isConfirmUndoIgnoreUserEnabled: StateFlow<Boolean> = _isConfirmUndoIgnoreUserEnabled.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage get() = userProfileServiceFacade::getUserProfileIcon

    private val observedChatMessages =
        MutableStateFlow<Set<BisqEasyOpenTradeMessage>>(emptySet())

    private var tradeJob: Job? = null

    fun initialize(tradeId: String) {
        // Reset with the job: re-initialising resolves another trade, and until it does, the previous
        // trade's messages and its not-found dialog do not describe what the screen is showing. The
        // delivery-status observers go with them: cancelling the job stops the collector but leaves the
        // observers on the previous trade's messages, and onViewUnattaching only runs on leaving.
        _uiState.value = TradeChatUiState()
        clearObservedChatMessages()

        tradeJob?.cancel()
        tradeJob =
            presenterScope.launch {
                launch { observeSettings() }
                launch { observeMyProfiles() }

                val currentTrade = tradesServiceFacade.selectOpenTradeWhenSynced(tradeId)
                if (currentTrade == null) {
                    log.w { "TradeChatPresenter.initialize could not resolve trade ${tradeId.take(8)}: absent from the synced open trades, or the sync failed - skipping flow collection" }
                    _uiState.update { it.copy(selectedTrade = null, isLoading = false, isTradeNotFound = true) }
                    return@launch
                }
                // Seed the stored count (or 0) with the trade so the list is not held on the
                // unread-unknown placeholder after loading finishes. The collector below keeps it live.
                val initialReadCount =
                    tradeReadStateRepository.data
                        .first()
                        .map
                        .getOrElse(tradeId) { 0 }
                _uiState.update { it.copy(selectedTrade = currentTrade, readCount = initialReadCount) }

                val bisqEasyOpenTradeChannelModel = currentTrade.bisqEasyOpenTradeChannelModel
                // cancel notifications of chat related to this trade
                notificationController.cancel(NotificationIds.getNewChatMessageId(currentTrade.shortTradeId))

                // Children of the trade's job, not of presenterScope: re-initialising with another trade
                // cancels them along with the wait that started them.
                launch {
                    // True once there is something to render, false once there is not going to be,
                    // null while it is still undecided.
                    val delivered =
                        combine(
                            bisqEasyOpenTradeChannelModel.chatMessages,
                            tradeChatMessagesServiceFacade.chatMessagesSynced,
                            tradeChatMessagesServiceFacade.chatMessagesSyncFailed,
                        ) { messages, synced, failed ->
                            when {
                                messages.isNotEmpty() || synced -> true
                                failed -> false
                                else -> null
                            }
                        }.first { it != null }
                    if (delivered == false) {
                        log.w { "Chat messages for trade ${tradeId.take(8)} are not coming, their subscription failed - rendering what has arrived" }
                    }
                    _uiState.update { it.copy(isLoading = false) }
                }

                launch {
                    bisqEasyOpenTradeChannelModel.chatMessages.collect { messages ->
                        observedChatMessages.update {
                            val newMessages = messages - it
                            newMessages.forEach { m ->
                                m.addMessageDeliveryStatusObserver(messageDeliveryServiceFacade)
                            }
                            messages
                        }
                    }
                }

                launch { observeMentionCandidates(bisqEasyOpenTradeChannelModel) }
                launch { observeReadCount(tradeId) }

                launch {
                    userProfileServiceFacade.ignoredProfileIds
                        .combine(bisqEasyOpenTradeChannelModel.chatMessages) { ignoredIds, messages ->
                            messages
                                .filter { message ->
                                    when (message.chatMessageType) {
                                        ChatMessageTypeEnum.TEXT, ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER ->
                                            !ignoredIds.contains(
                                                message.senderUserProfileId,
                                            )

                                        else -> true
                                    }
                                }.toList()
                                .sortedByDescending { it.date } to ignoredIds
                        }.collect { (messages, ignoredIds) ->
                            _uiState.update {
                                it.copy(
                                    messages = messages,
                                    ignoredProfileIds = ignoredIds,
                                )
                            }
                            // Load user profile icons off the main thread to avoid
                            // blocking UI rendering (iOS CA Fence hang prevention)
                            withContext(Dispatchers.IO) {
                                for (message in messages) {
                                    val userProfile = message.senderUserProfile
                                    if (_userProfileIconByProfileId.value[userProfile.id] == null) {
                                        val image =
                                            userProfileServiceFacade.getUserProfileIcon(
                                                userProfile,
                                            )
                                        _userProfileIconByProfileId.update { it + (userProfile.id to image) }
                                    }
                                }
                            }
                        }
                }
            }
    }

    fun onAction(action: TradeChatUiAction) {
        when (action) {
            is TradeChatUiAction.OnSendMessage -> sendChatMessage(action.text)
            is TradeChatUiAction.OnResendMessage -> messageDeliveryServiceFacade.onResendMessage(action.messageId)
            is TradeChatUiAction.OnAddReaction -> onAddReaction(action.message, action.reaction)
            is TradeChatUiAction.OnRemoveReaction -> onRemoveReaction(action.message, action.reaction)
            is TradeChatUiAction.OnReply -> _uiState.update { it.copy(quotedMessage = action.message) }

            is TradeChatUiAction.OnPeerProfileClick -> navigateTo(NavRoute.PeerProfile(action.profileId))

            is TradeChatUiAction.OnIgnoreUserClick ->
                _uiState.update { it.copy(ignoreTargetProfileId = action.profileId) }

            TradeChatUiAction.OnConfirmIgnore -> onConfirmedIgnoreUser()
            TradeChatUiAction.OnDismissIgnoreDialog ->
                _uiState.update { it.copy(ignoreTargetProfileId = null) }

            is TradeChatUiAction.OnUndoIgnoreUserClick ->
                _uiState.update { it.copy(undoIgnoreTargetProfileId = action.profileId) }

            TradeChatUiAction.OnConfirmUndoIgnore -> onConfirmedUndoIgnoreUser()
            TradeChatUiAction.OnDismissUndoIgnoreDialog ->
                _uiState.update { it.copy(undoIgnoreTargetProfileId = null) }

            is TradeChatUiAction.OnReportUserClick -> onReportUserClick(action.message)

            TradeChatUiAction.OnDismissReportDialog ->
                _uiState.update {
                    it.copy(reportTargetMessage = null, reportDraft = null, reportDraftProfileId = null)
                }

            is TradeChatUiAction.OnReportFailure -> onReportUserError(action.reportMessage)

            TradeChatUiAction.OnOpenChatRules -> navigateTo(NavRoute.ChatRules)

            TradeChatUiAction.OnDontShowAgainChatRulesWarningBox ->
                presenterScope.launch { settingsRepository.setShowChatRulesWarnBox(false) }

            is TradeChatUiAction.OnUpdateReadCount -> onUpdateReadCount(action.count)

            TradeChatUiAction.OnTradeNotFoundDialogDismiss -> {
                _uiState.update { it.copy(isTradeNotFound = false) }
                navigateBack()
            }
        }
    }

    /**
     * Separate from the ignore-filtered [TradeChatUiState.messages] collector: candidates must stay
     * on the raw channel set. Desktop offers ignored authors too. `traders` holds only the
     * peer when I trade (both traders when I mediate), so my own side comes from
     * [BisqEasyOpenTradeChannel.myUserIdentity] — the one identity this trade runs with.
     */
    private suspend fun observeMentionCandidates(channel: BisqEasyOpenTradeChannel) {
        channel.chatMessages.collect { messages ->
            _uiState.update {
                it.copy(
                    mentionCandidates =
                        deriveMentionCandidates(
                            messages,
                            participants = channel.traders + listOfNotNull(channel.mediator) + channel.myUserIdentity.userProfile,
                        ),
                )
            }
        }
    }

    private suspend fun observeSettings() {
        settingsRepository.data.collect { settings ->
            _uiState.update { it.copy(showChatRulesWarnBox = settings.showChatRulesWarnBox) }
        }
    }

    private suspend fun observeMyProfiles() {
        userProfileServiceFacade.userProfiles.collect { owned ->
            _uiState.update { it.copy(myProfiles = owned) }
        }
    }

    private suspend fun observeReadCount(tradeId: String) {
        tradeReadStateRepository.data.collect { readStates ->
            _uiState.update { it.copy(readCount = readStates.map.getOrElse(tradeId) { 0 }) }
        }
    }

    override fun onViewUnattaching() {
        _userProfileIconByProfileId.update { emptyMap() }
        clearObservedChatMessages()
        super.onViewUnattaching()
    }

    private fun clearObservedChatMessages() {
        observedChatMessages.update {
            it.forEach { m -> m.removeMessageDeliveryStatusObserver(messageDeliveryServiceFacade) }
            emptySet()
        }
    }

    private fun sendChatMessage(text: String) {
        val finalText = text.trim()
        if (finalText.isEmpty()) return

        val citation =
            _uiState.value.quotedMessage?.let { quotedMessage ->
                quotedMessage.text?.let { text ->
                    Citation(
                        quotedMessage.senderUserProfileId,
                        text,
                        quotedMessage.id,
                    )
                }
            }
        guardedSuspendAction(_isSendChatMessageEnabled, "sendChatMessage") {
            tradeChatMessagesServiceFacade
                .sendChatMessage(finalText, citation)
                .onSuccess {
                    _uiState.update { it.copy(quotedMessage = null) }
                }
        }
    }

    suspend fun getUserName(peerProfileId: String): String = userProfileServiceFacade.findUserProfile(peerProfileId)?.userName ?: "data.na".i18n()

    private fun onAddReaction(
        message: BisqEasyOpenTradeMessage,
        reaction: ReactionEnum,
    ) {
        presenterScope.launch {
            tradeChatMessagesServiceFacade.addChatMessageReaction(message.id, reaction)
        }
    }

    private fun onRemoveReaction(
        message: BisqEasyOpenTradeMessage,
        reaction: BisqEasyOpenTradeMessageReaction,
    ) {
        presenterScope.launch {
            tradeChatMessagesServiceFacade.removeChatMessageReaction(message.id, reaction)
        }
    }

    private fun onConfirmedIgnoreUser() {
        val id = _uiState.value.ignoreTargetProfileId ?: return
        guardedSuspendAction(_isConfirmIgnoreUserEnabled, "onConfirmedIgnoreUser") {
            try {
                userProfileServiceFacade.ignoreUserProfile(id)
                _uiState.update { it.copy(ignoreTargetProfileId = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to ignore user ${id.take(8)}" }
            }
        }
    }

    private fun onConfirmedUndoIgnoreUser() {
        val id = _uiState.value.undoIgnoreTargetProfileId ?: return
        guardedSuspendAction(_isConfirmUndoIgnoreUserEnabled, "onConfirmedUndoIgnoreUser") {
            try {
                userProfileServiceFacade.undoIgnoreUserProfile(id)
                _uiState.update { it.copy(undoIgnoreTargetProfileId = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to undo ignore user ${id.take(8)}" }
            }
        }
    }

    /**
     * Restore a failed draft only for the same accused profile. Opening a report on a
     * different sender clears it so `ReportUserDialog` is not seeded with stale text.
     */
    private fun onReportUserClick(message: BisqEasyOpenTradeMessage) {
        _uiState.update { state ->
            val sameTarget = state.reportDraftProfileId == message.senderUserProfileId
            state.copy(
                reportTargetMessage = message,
                reportDraft = if (sameTarget) state.reportDraft else null,
                reportDraftProfileId = if (sameTarget) state.reportDraftProfileId else null,
            )
        }
    }

    /**
     * Keeps the typed report so the dialog can be reopened with it. The error snackbar belongs to
     * `ReportUserPresenter` — raising a second one here would double it. A failure with no
     * open target is leftover from a dismissed dialog and must not retain an unowned draft.
     */
    private fun onReportUserError(reportMessage: String) {
        val targetId = _uiState.value.reportTargetMessage?.senderUserProfileId ?: return
        _uiState.update {
            it.copy(
                reportTargetMessage = null,
                reportDraft = reportMessage,
                reportDraftProfileId = targetId,
            )
        }
    }

    private fun onUpdateReadCount(newValue: Int) {
        val tradeId = _uiState.value.selectedTrade?.tradeId ?: return

        presenterScope.launch {
            withContext(Dispatchers.IO) {
                tradeReadStateRepository.setCount(tradeId, newValue)
            }
        }
    }
}
