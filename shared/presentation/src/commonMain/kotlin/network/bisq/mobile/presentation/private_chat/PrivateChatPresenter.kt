package network.bisq.mobile.presentation.private_chat

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationIds
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

@OptIn(FlowPreview::class)
class PrivateChatPresenter(
    mainPresenter: MainPresenter,
    private val privateChatServiceFacade: PrivateChatServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val notificationController: NotificationController,
    private val settingsRepository: SettingsRepository,
) : BasePresenter(mainPresenter) {
    private companion object {
        val ZERO_REPUTATION = ReputationScoreVO(totalScore = 0L, fiveSystemScore = 0.0, ranking = 0)

        /** Covers a subscription round-trip over Tor without leaving the screen blank on a real miss. */
        const val CHANNEL_WAIT_TIMEOUT_MS = 5_000L

        /** Coalesces the scroll-driven read updates into one node round-trip. */
        const val CONSUME_NOTIFICATIONS_DEBOUNCE_MS = 500L
    }

    private val _uiState = MutableStateFlow(PrivateChatUiState())
    val uiState: StateFlow<PrivateChatUiState> = _uiState.asStateFlow()

    private val _isSendChatMessageEnabled = MutableStateFlow(true)
    val isSendChatMessageEnabled: StateFlow<Boolean> = _isSendChatMessageEnabled.asStateFlow()

    private val _isLeaveChatEnabled = MutableStateFlow(true)
    val isLeaveChatEnabled: StateFlow<Boolean> = _isLeaveChatEnabled.asStateFlow()

    private val _isConfirmIgnoreUserEnabled = MutableStateFlow(true)
    val isConfirmIgnoreUserEnabled: StateFlow<Boolean> = _isConfirmIgnoreUserEnabled.asStateFlow()

    private val _isConfirmUndoIgnoreUserEnabled = MutableStateFlow(true)
    val isConfirmUndoIgnoreUserEnabled: StateFlow<Boolean> = _isConfirmUndoIgnoreUserEnabled.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    private var initializedChannelId: String? = null
    private var channelJob: Job? = null

    /** Extra buffer + DROP_OLDEST so the non-suspending [onUpdateReadCount] can never block or lose the latest. */
    private val readCountUpdates =
        MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * What `ChatMessageList` last reported as read, or null before it reports anything.
     *
     * Held here rather than derived from [TwoPartyPrivateChatChannel.unreadCount], because opening
     * the thread consumes the channel and drives that count to zero — it can seed the count but
     * cannot track it afterwards. Trade chat keeps the same state in `TradeReadStateRepository`;
     * a DM has no equivalent to persist to, and does not need one: Bisq 2 already remembers that
     * the conversation was consumed.
     */
    private val reportedReadCount = MutableStateFlow<Int?>(null)

    /**
     * Idempotent on [channelId], because the screen's `LaunchedEffect` re-fires whenever this
     * destination is revealed from the back stack; reloading then would flash the loading state.
     */
    fun initialize(channelId: String) {
        if (initializedChannelId == channelId) return
        initializedChannelId = channelId
        _uiState.value = PrivateChatUiState(channelId = channelId)
        reportedReadCount.value = null

        notificationController.cancel(NotificationIds.getNewPrivateChatMessageId(channelId))

        channelJob?.cancel()
        channelJob =
            presenterScope.launch {
                val channel = awaitChannel(channelId)
                if (channel == null) {
                    log.w { "No private chat channel found for id $channelId" }
                    _uiState.update { it.copy(isChannelNotFound = true, isLoading = false) }
                    return@launch
                }
                // Read before consuming, because consuming zeroes it — synchronously on the node
                // flavour, where Bisq 2 publishes changedNotification from inside consume(). Reading
                // it afterwards would always yield 0, so the divider would never render.
                val unreadOnOpen = channel.unreadCount.value
                // Desktop consumes on selection, so opening the thread is what marks it read — but as
                // a subscription side effect, never before rendering (ChatMessagesListController).
                // Awaiting it here put a websocket round-trip bounded by TOR_CONNECT_TIMEOUT in front
                // of a channel that had already resolved, holding the screen on the loading state.
                presenterScope.launch { privateChatServiceFacade.consumeNotifications(channelId) }
                loadPeer(channel)
                observeMessages(channel, unreadOnOpen)
            }

        presenterScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update { it.copy(showChatRulesWarnBox = settings.showChatRulesWarnBox) }
            }
        }

        observeReadCountUpdates()
    }

    fun onAction(action: PrivateChatUiAction) {
        when (action) {
            is PrivateChatUiAction.OnSendMessage -> sendChatMessage(action.text)
            is PrivateChatUiAction.OnAddReaction -> onAddReaction(action)
            is PrivateChatUiAction.OnRemoveReaction -> onRemoveReaction(action)
            is PrivateChatUiAction.OnReply -> _uiState.update { it.copy(quotedMessage = action.message) }

            PrivateChatUiAction.OnPeerHeaderClick ->
                _uiState.value.peerUserProfile?.let { navigateTo(NavRoute.PeerProfile(it.id)) }

            is PrivateChatUiAction.OnPeerProfileClick -> navigateTo(NavRoute.PeerProfile(action.profileId))

            is PrivateChatUiAction.OnIgnoreUserClick -> _uiState.update { it.copy(ignoreUserId = action.profileId) }
            PrivateChatUiAction.OnConfirmIgnore -> onConfirmIgnore()
            PrivateChatUiAction.OnDismissIgnoreDialog -> _uiState.update { it.copy(ignoreUserId = "") }

            is PrivateChatUiAction.OnUndoIgnoreUserClick -> _uiState.update { it.copy(undoIgnoreUserId = action.profileId) }
            PrivateChatUiAction.OnConfirmUndoIgnore -> onConfirmUndoIgnore()
            PrivateChatUiAction.OnDismissUndoIgnoreDialog -> _uiState.update { it.copy(undoIgnoreUserId = "") }

            is PrivateChatUiAction.OnReportUserClick ->
                _uiState.update {
                    it.copy(showReportDialog = true, reportTargetProfile = action.message.senderUserProfile)
                }

            PrivateChatUiAction.OnDismissReportDialog ->
                _uiState.update { it.copy(showReportDialog = false, reportDraft = null) }

            is PrivateChatUiAction.OnReportFailure -> onReportFailure(action.reportMessage)

            PrivateChatUiAction.OnLeaveChatClick -> _uiState.update { it.copy(showLeaveConfirmDialog = true) }
            PrivateChatUiAction.OnConfirmLeave -> onConfirmLeave()
            PrivateChatUiAction.OnDismissLeaveDialog -> _uiState.update { it.copy(showLeaveConfirmDialog = false) }

            PrivateChatUiAction.OnOpenChatRules -> navigateTo(NavRoute.ChatRules)

            PrivateChatUiAction.OnDontShowAgainChatRulesWarningBox ->
                presenterScope.launch { settingsRepository.setShowChatRulesWarnBox(false) }

            is PrivateChatUiAction.OnUpdateReadCount -> onUpdateReadCount(action.count)

            PrivateChatUiAction.OnChannelNotFoundDialogDismiss -> {
                _uiState.update { it.copy(isChannelNotFound = false) }
                navigateBack()
            }
        }
    }

    suspend fun getUserName(peerProfileId: String): String = userProfileServiceFacade.findUserProfile(peerProfileId)?.userName ?: "data.na".i18n()

    // Private

    /**
     * Waits briefly rather than reading a snapshot. `PeerProfilePresenter` navigates here as soon as
     * `findOrCreateChannel` returns, but on the client flavour the channel only reaches
     * [PrivateChatServiceFacade.channels] via the `PRIVATE_CHAT_CHANNELS` subscription, collected on
     * a different coroutine — so a first-ever DM can arrive here before its own channel does.
     *
     * A timeout still means genuinely absent (e.g. left from another device), which is what the
     * not-found dialog is for.
     */
    private suspend fun awaitChannel(channelId: String): TwoPartyPrivateChatChannel? =
        withTimeoutOrNull(CHANNEL_WAIT_TIMEOUT_MS) {
            privateChatServiceFacade.channels
                .mapNotNull { channels -> channels.find { it.id == channelId } }
                .first()
        }

    private suspend fun loadPeer(channel: TwoPartyPrivateChatChannel) {
        val reputation =
            runCatching { reputationServiceFacade.getReputation(channel.peer.id).getOrNull() }
                .getOrNull() ?: ZERO_REPUTATION
        _uiState.update {
            it.copy(
                peerUserProfile = channel.peer,
                peerName = channel.peer.userName,
                peerStarRating = reputation.fiveSystemScore,
                isLoading = false,
            )
        }
    }

    /**
     * No delivery-status observer is registered, deliberately — this matches Bisq 2 desktop, which
     * shows no delivery state on a DM either.
     *
     * Desktop builds the widget for a DM (`ChatMessageListCellFactory` hands a own message to
     * `MyTextMessageBox`, which owns a `MessageDeliveryStatusBox`) and registers the status observer,
     * because `PrivateChatMessage.getAckRequestingMessageId()` is the plain message id and matches. But
     * `ChatMessageListItem.updateMessageStatus` opens with `if (peersProfileId == null) return`, and
     * that id is only ever parsed out of a `BisqEasyOpenTradeMessage`'s composite ack id — so the map
     * stays null and the box stays hidden. Delivery state for DMs has never shipped on any client.
     *
     * Mobile could not show it anyway: the node facade resolves message ids against
     * `bisqEasyOpenTradeChannelService` only, so a DM id misses and it logs a warning per message, and
     * the client implementation is a stub. Wiring it would buy a log line and no UI. Register it here
     * once upstream keys a two-party channel and a facade on both flavours can report on one.
     *
     * coroutineScope, so the collector below is a child of the caller's job rather than of
     * presenterScope: initialize() cancels channelJob on re-entry, and a collector launched outside it
     * would survive that and keep observing a channel we no longer show.
     */
    private suspend fun observeMessages(
        channel: TwoPartyPrivateChatChannel,
        unreadOnOpen: Long,
    ) = coroutineScope {
        combine(
            channel.chatMessages,
            userProfileServiceFacade.ignoredProfileIds,
            reportedReadCount,
        ) { messages, ignoredIds, reportedCount ->
            // ChatMessageList wants a *read* count, Bisq2 stores an *unread* one, and no exact
            // conversion exists. Bisq2 skips a message whose sender was ignored *at the moment it
            // arrived* (ChatNotificationService.onMessageAdded), but keeps notifications raised before
            // an ignore — so its count neither includes every ignored sender nor excludes them all,
            // and the model cannot tell the two apart.
            // So take the newest N over all messages as the unread set, then count the displayed
            // messages outside it. Approximate either way, but it can never put readCount past the
            // displayed list, which is what would make ChatMessageList compute a negative unread.
            val sortedByNewest = messages.sortedByDescending { it.date }
            val unreadIds = sortedByNewest.take(unreadOnOpen.toInt()).mapTo(mutableSetOf()) { it.id }
            val displayed = sortedByNewest.filterNot { ignoredIds.contains(it.senderUserProfileId) }
            // Once the list has reported a count it owns the number: it is the only side that knows
            // how far the user scrolled. Until then the count is derived from the state at open.
            val readCount = reportedCount ?: displayed.count { it.id !in unreadIds }
            Triple(
                displayed,
                ignoredIds,
                readCount.coerceIn(0, displayed.size),
            )
        }.collect { (messages, ignoredIds, readCount) ->
            _uiState.update {
                it.copy(
                    messages = messages,
                    ignoredProfileIds = ignoredIds,
                    readCount = readCount,
                )
            }
        }
    }

    private fun sendChatMessage(text: String) {
        val finalText = text.trim()
        if (finalText.isEmpty()) return
        val channelId = _uiState.value.channelId
        val citation =
            _uiState.value.quotedMessage?.let { quoted ->
                quoted.text?.let { Citation(quoted.senderUserProfileId, it, quoted.id) }
            }
        guardedSuspendAction(_isSendChatMessageEnabled, "sendChatMessage") {
            privateChatServiceFacade
                .sendChatMessage(channelId, finalText, citation)
                .onSuccess { _uiState.update { it.copy(quotedMessage = null) } }
                // Surfaced, not just logged: `ChatInputField` clears its text as soon as it hands the
                // message over, so a failure here silently loses what the user typed. The snackbar is
                // the only signal they get. handleError, so a timeout reads as a timeout.
                .onFailure { handleError(it) }
        }
    }

    private fun onAddReaction(action: PrivateChatUiAction.OnAddReaction) {
        presenterScope.launch {
            privateChatServiceFacade.addChatMessageReaction(
                _uiState.value.channelId,
                action.message.id,
                action.reaction,
            )
        }
    }

    private fun onRemoveReaction(action: PrivateChatUiAction.OnRemoveReaction) {
        presenterScope.launch {
            privateChatServiceFacade.removeChatMessageReaction(
                _uiState.value.channelId,
                action.message.id,
                action.reaction,
            )
        }
    }

    private fun onConfirmIgnore() {
        val profileId = _uiState.value.ignoreUserId
        if (profileId.isEmpty()) return
        guardedSuspendAction(_isConfirmIgnoreUserEnabled, "onConfirmIgnore") {
            try {
                userProfileServiceFacade.ignoreUserProfile(profileId)
            } catch (e: Exception) {
                log.e(e) { "Failed to ignore user $profileId" }
            }
            _uiState.update { it.copy(ignoreUserId = "") }
        }
    }

    private fun onConfirmUndoIgnore() {
        val profileId = _uiState.value.undoIgnoreUserId
        if (profileId.isEmpty()) return
        guardedSuspendAction(_isConfirmUndoIgnoreUserEnabled, "onConfirmUndoIgnore") {
            try {
                userProfileServiceFacade.undoIgnoreUserProfile(profileId)
            } catch (e: Exception) {
                log.e(e) { "Failed to undo ignore user $profileId" }
            }
            _uiState.update { it.copy(undoIgnoreUserId = "") }
        }
    }

    /** No snackbar here — `ReportUserPresenter` has already shown one for the same failure. */
    private fun onReportFailure(reportMessage: String) {
        _uiState.update { it.copy(showReportDialog = false, reportDraft = reportMessage) }
    }

    private fun onConfirmLeave() {
        val channelId = _uiState.value.channelId
        guardedSuspendAction(_isLeaveChatEnabled, "onConfirmLeave") {
            privateChatServiceFacade
                .leaveChannel(channelId)
                .onSuccess {
                    _uiState.update { it.copy(showLeaveConfirmDialog = false) }
                    navigateBack()
                }.onFailure {
                    log.e(it) { "Failed to leave private chat channel $channelId" }
                    _uiState.update { state -> state.copy(showLeaveConfirmDialog = false) }
                    handleError(it)
                }
        }
    }

    /**
     * Two effects, on purpose. [count] keeps the unread divider anchored while the user scrolls,
     * locally. The node round-trip marks the whole conversation consumed, because Bisq 2 has no
     * partial consume — so it is debounced: `ChatMessageList` raises this on every scroll.
     */
    private fun onUpdateReadCount(count: Int) {
        reportedReadCount.value = count
        val channelId = _uiState.value.channelId
        if (channelId.isEmpty()) return
        readCountUpdates.tryEmit(channelId)
    }

    private fun observeReadCountUpdates() {
        presenterScope.launch {
            readCountUpdates
                .debounce(CONSUME_NOTIFICATIONS_DEBOUNCE_MS)
                .collect { channelId -> privateChatServiceFacade.consumeNotifications(channelId) }
        }
    }
}
