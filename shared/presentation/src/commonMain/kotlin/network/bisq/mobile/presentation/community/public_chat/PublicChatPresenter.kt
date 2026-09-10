package network.bisq.mobile.presentation.community.public_chat

import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.chat.public_chat.PublicChatNotAuthorException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatRemovalRejectedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRefusedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRejection
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * A public chat thread, parameterized by domain so the same screen serves the hub's Discussions
 * segment and the pushed Support screen.
 *
 * Two things separate it from `PrivateChatPresenter`, and both are places a copy would misbehave in
 * silence: the channel is resolved by DOMAIN rather than by position or by a literal id, because the
 * facade really serves two channels; and ignore, report and profile targets are per message, because
 * a public channel has no fixed peer. There is also no leave action, by design.
 */
@OptIn(FlowPreview::class)
class PublicChatPresenter(
    mainPresenter: MainPresenter,
    private val publicChatServiceFacade: PublicChatServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val chatChannelDomain: ChatChannelDomainEnum,
) : BasePresenter(mainPresenter) {
    private companion object {
        /** Coalesces the scroll-driven read updates into one node round-trip. */
        const val CONSUME_NOTIFICATIONS_DEBOUNCE_MS = 500L
    }

    private val _uiState = MutableStateFlow(PublicChatUiState())
    val uiState: StateFlow<PublicChatUiState> = _uiState.asStateFlow()

    private val _isSendChatMessageEnabled = MutableStateFlow(true)
    val isSendChatMessageEnabled: StateFlow<Boolean> = _isSendChatMessageEnabled.asStateFlow()

    private val _isAddReactionEnabled = MutableStateFlow(true)
    private val _isRemoveReactionEnabled = MutableStateFlow(true)
    private val _isDeleteMessageEnabled = MutableStateFlow(true)

    /** One guard for both dialogs: the message menu offers ignore or undo, never both. */
    private val _isIgnoreActionEnabled = MutableStateFlow(true)
    val isIgnoreActionEnabled: StateFlow<Boolean> = _isIgnoreActionEnabled.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    private var channelJob: Job? = null

    /** Extra buffer + DROP_OLDEST so the non-suspending [onUpdateReadCount] can never block or lose the latest. */
    private val readCountUpdates =
        MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * What `ChatMessageList` last reported as read, or null before it reports anything. Held here
     * rather than derived from the channel's unread count, because opening the thread consumes the
     * channel and drives that count to zero — it can seed the count but cannot track it afterwards.
     */
    private val reportedReadCount = MutableStateFlow<Int?>(null)

    private val searchQuery = MutableStateFlow("")

    /**
     * One presenter, two screens, and three chat domains it is never mounted on — the last of which
     * is why this is nullable. The domain arrives at construction, so the answer is settled before
     * the view can attach and ask.
     */
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened? =
        when (chatChannelDomain) {
            ChatChannelDomainEnum.DISCUSSION -> AnalyticsEvent.ScreenOpened.CommunityDiscussions
            ChatChannelDomainEnum.SUPPORT -> AnalyticsEvent.ScreenOpened.CommunitySupport
            else -> null
        }

    /**
     * The only place the collectors start. `presenterScope` is cancelled on detach and recreated on
     * the next attach, so a tab or screen coming back needs a fresh job; [startChannelJob] cancels
     * whatever was there first, which is all an attach has to do.
     */
    override fun onViewAttached() {
        super.onViewAttached()
        startChannelJob()
    }

    private fun startChannelJob() {
        channelJob?.cancel()
        channelJob =
            presenterScope.launch {
                // Children of the channel's job, so a restart on re-attach takes them down with
                // everything else the previous one started.
                launch {
                    settingsRepository.data.collect { settings ->
                        _uiState.update { it.copy(showChatRulesWarnBox = settings.showChatRulesWarnBox) }
                    }
                }
                launch {
                    publicChatServiceFacade.isSupported.collect { isSupported ->
                        _uiState.update { it.copy(isSupported = isSupported) }
                    }
                }
                launch { observeMyProfiles() }
                launch { observeReadCountUpdates() }
                // Disabled until the channel resolves: the composer is rendered outside the loading
                // branch, and it clears its text the moment it hands the message over — so an early
                // send costs the user what they wrote rather than just failing.
                _isSendChatMessageEnabled.value = false
                val channel = awaitChannel()
                _isSendChatMessageEnabled.value = true
                // Read before consuming, because consuming zeroes it — synchronously on the node,
                // where bisq2 publishes changedNotification from inside consume(). Reading afterwards
                // would always yield 0 and the unread divider would never render.
                val unreadOnOpen = channel.unreadCount.value
                _uiState.update {
                    it.copy(
                        channelId = channel.id,
                        isLoading = false,
                    )
                }
                // Not awaited before rendering: on Bisq Connect this is a round trip that would hold
                // the screen on the loading state behind a channel that had already resolved. Guarded
                // like the debounced collector below, and for the same reason.
                launch { consumeNotificationsQuietly(channel.id) }
                observeMessages(channel, unreadOnOpen)
            }
    }

    fun onAction(action: PublicChatUiAction) {
        when (action) {
            is PublicChatUiAction.OnSendMessage -> onSendMessage(action.text)
            is PublicChatUiAction.OnAddReaction -> onAddReaction(action)
            is PublicChatUiAction.OnRemoveReaction -> onRemoveReaction(action)
            is PublicChatUiAction.OnReply -> _uiState.update { it.copy(quotedMessage = action.message) }

            is PublicChatUiAction.OnEditMessage ->
                _uiState.update {
                    // The quote goes: bisq2 keeps the original's citation on an edit, so there is
                    // nothing for a quote banner to offer while editing.
                    it.copy(
                        editingMessageId = action.message.id,
                        editingInitialText = action.message.textString,
                        quotedMessage = null,
                    )
                }

            PublicChatUiAction.OnCancelEdit -> clearEditing()

            is PublicChatUiAction.OnDeleteMessageClick ->
                _uiState.update { it.copy(deleteTargetMessageId = action.message.id) }

            PublicChatUiAction.OnConfirmDelete -> onConfirmDelete()
            PublicChatUiAction.OnDismissDeleteDialog -> _uiState.update { it.copy(deleteTargetMessageId = null) }

            is PublicChatUiAction.OnSearchQueryChange -> onSearchQueryChange(action.query)

            is PublicChatUiAction.OnPeerProfileClick -> navigateTo(NavRoute.PeerProfile(action.profileId))

            is PublicChatUiAction.OnIgnoreUserClick ->
                _uiState.update { it.copy(ignoreTargetProfileId = action.profileId) }

            PublicChatUiAction.OnConfirmIgnore -> onConfirmIgnore()
            PublicChatUiAction.OnDismissIgnoreDialog -> _uiState.update { it.copy(ignoreTargetProfileId = null) }

            is PublicChatUiAction.OnUndoIgnoreUserClick ->
                _uiState.update { it.copy(undoIgnoreTargetProfileId = action.profileId) }

            PublicChatUiAction.OnConfirmUndoIgnore -> onConfirmUndoIgnore()

            PublicChatUiAction.OnDismissUndoIgnoreDialog ->
                _uiState.update { it.copy(undoIgnoreTargetProfileId = null) }

            is PublicChatUiAction.OnReportUserClick ->
                _uiState.update { it.copy(reportTargetUserProfile = action.message.senderUserProfile) }

            PublicChatUiAction.OnDismissReportDialog ->
                _uiState.update { it.copy(reportTargetUserProfile = null, reportDraft = null) }

            is PublicChatUiAction.OnReportFailure ->
                // No snackbar: `ReportUserPresenter` has already shown one for the same failure.
                _uiState.update { it.copy(reportTargetUserProfile = null, reportDraft = action.reportMessage) }

            PublicChatUiAction.OnOpenChatRules -> navigateTo(NavRoute.ChatRules)

            PublicChatUiAction.OnDontShowAgainChatRulesWarningBox ->
                presenterScope.launch { settingsRepository.setShowChatRulesWarnBox(false) }

            is PublicChatUiAction.OnUpdateReadCount -> onUpdateReadCount(action.count)
        }
    }

    /** See `PrivateChatPresenter.getUserName`: this is handed to `ChatMessageList` and must not throw. */
    suspend fun getUserName(profileId: String): String {
        val userProfile =
            try {
                userProfileServiceFacade.findUserProfile(profileId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w(e) { "Failed to resolve a user name for a chat message" }
                null
            }
        return userProfile?.userName ?: "data.na".i18n()
    }

    // Private

    /**
     * By domain, never by position or by the literal `"discussion.bisq"`: the facade serves two
     * channels, so `firstOrNull()` would pick Support in silence, and a literal id would turn an
     * upstream rename into a blank screen.
     *
     * Waits indefinitely rather than timing out. A timeout is a node-shaped assumption — there the
     * channel is in memory before the facade activates — while on Bisq Connect the same code waits
     * for a WebSocket subscribe and its snapshot over Tor on a cold start, and would routinely render
     * "the channel could not be loaded" against a healthy node. The terminal states are
     * [PublicChatUiState.isSupported] and an explicit facade failure, never a clock.
     */
    private suspend fun awaitChannel(): CommonPublicChatChannel =
        publicChatServiceFacade.channels
            .mapNotNull { channels -> channels.firstOrNull { it.chatChannelDomain == chatChannelDomain } }
            .first()

    private suspend fun observeMessages(
        channel: CommonPublicChatChannel,
        unreadOnOpen: Long,
    ) = coroutineScope {
        combine(
            channel.chatMessages,
            userProfileServiceFacade.ignoredProfileIds,
            searchQuery.map { it.trim() },
            reportedReadCount,
        ) { messages, ignoredIds, query, reportedCount ->
            // Sort once, then filter, never the reverse: the id tie-break is what keeps the keyed
            // LazyColumn from reordering visibly when senders share a millisecond, and it has to be
            // applied to the whole set before anything is dropped. Same comparator as bisq2's
            // PublicChatDtoFactory.NEWEST_FIRST.
            val sortedByNewest = messages.sortedWith(compareByDescending<CommonPublicChatMessage> { it.date }.thenByDescending { it.id })
            val displayed = sortedByNewest.filterNot { it.senderUserProfileId in ignoredIds }
            // Only the unfiltered list feeds the read count. While searching the list must not mark
            // anything read, so readCount is pinned to the size of what is shown: ChatMessageList
            // derives both the unread divider and the jump-to-bottom badge from the difference.
            if (query.isNotEmpty()) {
                val matches = displayed.filter { it.textString.contains(query, ignoreCase = true) }
                MessagesState(matches, ignoredIds, matches.size, matches.size)
            } else {
                // Approximate on purpose: the seed counts every notification bisq2 holds for the
                // channel, while `displayed` has already dropped ignored senders, banned authors and
                // expired messages. Unread messages from someone you ignore therefore push the
                // divider a row too high, until the list reports its own count and takes over below.
                val unreadIds = displayed.take(unreadOnOpen.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()).mapTo(mutableSetOf()) { it.id }
                // Once the list has reported a count it owns the number: it is the only side that
                // knows how far the user scrolled.
                val readCount = reportedCount ?: displayed.count { it.id !in unreadIds }
                MessagesState(displayed, ignoredIds, readCount.coerceIn(0, displayed.size), 0)
            }
        }.collect { state ->
            _uiState.update {
                it.copy(
                    messages = state.messages,
                    ignoredProfileIds = state.ignoredProfileIds,
                    readCount = state.readCount,
                    searchMatchCount = state.searchMatchCount,
                )
            }
        }
    }

    private suspend fun observeMyProfiles() {
        userProfileServiceFacade.userProfiles.collect { owned ->
            _uiState.update { it.copy(myProfiles = owned) }
        }
    }

    private data class MessagesState(
        val messages: List<CommonPublicChatMessage>,
        val ignoredProfileIds: Set<String>,
        val readCount: Int,
        val searchMatchCount: Int,
    )

    private fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** The composer has one button, so a send while editing is a save. */
    private fun onSendMessage(text: String) {
        val finalText = text.trim()
        if (finalText.isEmpty()) return
        val state = _uiState.value
        val editingMessageId = state.editingMessageId
        if (editingMessageId != null) {
            onSaveEdit(state.channelId, editingMessageId, finalText)
            return
        }
        val citation =
            state.quotedMessage?.let { quoted ->
                quoted.text?.let { Citation(quoted.senderUserProfileId, it, quoted.id) }
            }
        guardedSuspendAction(_isSendChatMessageEnabled, "sendChatMessage") {
            publicChatServiceFacade
                .sendChatMessage(state.channelId, finalText, citation)
                .onSuccess { _uiState.update { it.copy(quotedMessage = null) } }
                // Surfaced, not just logged: the composer clears its text as soon as it hands the
                // message over, so a failure here silently loses what the user typed.
                .onFailure { handleError(it, customHandler = ::showPublicChatFailure) }
        }
    }

    private fun onSaveEdit(
        channelId: String,
        messageId: String,
        text: String,
    ) {
        guardedSuspendAction(_isSendChatMessageEnabled, "editChatMessage") {
            publicChatServiceFacade
                .editChatMessage(channelId, messageId, text)
                .onSuccess { clearEditing() }
                .onFailure { handleError(it, customHandler = ::showPublicChatFailure) }
        }
    }

    private fun clearEditing() {
        _uiState.update { it.copy(editingMessageId = null, editingInitialText = "") }
    }

    private fun onConfirmDelete() {
        val state = _uiState.value
        val messageId = state.deleteTargetMessageId ?: return
        guardedSuspendAction(_isDeleteMessageEnabled, "deleteChatMessage") {
            publicChatServiceFacade
                .deleteChatMessage(state.channelId, messageId)
                .onFailure { handleError(it, customHandler = ::showPublicChatFailure) }
            _uiState.update { it.copy(deleteTargetMessageId = null) }
        }
    }

    /**
     * Guarded without the loading overlay: an overlay per emoji tap is unusable, and one guard per
     * action so an add followed by a remove still runs both.
     */
    private fun onAddReaction(action: PublicChatUiAction.OnAddReaction) {
        guardedSuspendAction(_isAddReactionEnabled, "onAddReaction", showLoadingOverlay = false) {
            publicChatServiceFacade
                .addChatMessageReaction(_uiState.value.channelId, action.message.id, action.reaction)
                .onFailure { handleError(it, customHandler = ::showPublicChatFailure) }
        }
    }

    private fun onRemoveReaction(action: PublicChatUiAction.OnRemoveReaction) {
        guardedSuspendAction(_isRemoveReactionEnabled, "onRemoveReaction", showLoadingOverlay = false) {
            publicChatServiceFacade
                .removeChatMessageReaction(_uiState.value.channelId, action.message.id, action.reaction)
                .onFailure { handleError(it, customHandler = ::showPublicChatFailure) }
        }
    }

    private fun onConfirmIgnore() {
        val profileId = _uiState.value.ignoreTargetProfileId ?: return
        confirmIgnoreAction("onConfirmIgnore", profileId, userProfileServiceFacade::ignoreUserProfile) {
            it.copy(ignoreTargetProfileId = null)
        }
    }

    private fun onConfirmUndoIgnore() {
        val profileId = _uiState.value.undoIgnoreTargetProfileId ?: return
        confirmIgnoreAction("onConfirmUndoIgnore", profileId, userProfileServiceFacade::undoIgnoreUserProfile) {
            it.copy(undoIgnoreTargetProfileId = null)
        }
    }

    private fun confirmIgnoreAction(
        name: String,
        profileId: String,
        call: suspend (String) -> Unit,
        close: (PublicChatUiState) -> PublicChatUiState,
    ) {
        guardedSuspendAction(_isIgnoreActionEnabled, name) {
            try {
                call(profileId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No log here: handleError logs the exception before it shows the snackbar.
                handleError(e)
            }
            _uiState.update(close)
        }
    }

    /**
     * The four things the facade can say that `handleError`'s default copy ("something went wrong,
     * try again") would send the user in circles over: none of them is a connection problem and no
     * retry fixes any of them.
     *
     * @return true when it handled the failure, which suppresses `handleError`'s own snackbar.
     */
    private fun showPublicChatFailure(exception: Throwable): Boolean {
        val message =
            when (exception) {
                is PublicChatSendRefusedException ->
                    when (exception.rejection) {
                        PublicChatSendRejection.MY_PROFILE_BANNED -> "mobile.community.chat.refused.myProfileBanned".i18n()
                        PublicChatSendRejection.RATE_LIMIT_EXCEEDED -> "mobile.community.chat.refused.rateLimitExceeded".i18n()
                        PublicChatSendRejection.UNKNOWN -> return false
                    }
                is PublicChatNotAuthorException -> "mobile.community.chat.notAuthor".i18n()
                is PublicChatRemovalRejectedException -> "mobile.community.chat.removalRejected".i18n()
                else -> return false
            }
        showSnackbar(message, type = SnackbarType.ERROR)
        return true
    }

    /**
     * Two effects, on purpose. [count] keeps the unread divider anchored while the user scrolls,
     * locally. The node round-trip marks the whole channel consumed, because bisq2 has no partial
     * consume — so it is debounced: `ChatMessageList` raises this on every scroll.
     *
     * Swallowed while searching: the list is showing matches rather than the conversation, so
     * scrolling it must not mark unread messages read.
     */
    private fun onUpdateReadCount(count: Int) {
        if (_uiState.value.isSearching) return
        reportedReadCount.value = count
        val channelId = _uiState.value.channelId
        if (channelId.isEmpty()) return
        readCountUpdates.tryEmit(channelId)
    }

    private suspend fun observeReadCountUpdates() {
        readCountUpdates
            .debounce(CONSUME_NOTIFICATIONS_DEBOUNCE_MS)
            .collect { channelId -> consumeNotificationsQuietly(channelId) }
    }

    /**
     * `consumeNotifications` returns Unit and reports failure by throwing, and one escaped throw would
     * cancel the collector for the rest of the screen's life — the channel would silently stop being
     * marked read.
     */
    private suspend fun consumeNotificationsQuietly(channelId: String) {
        try {
            publicChatServiceFacade.consumeNotifications(channelId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Failed to mark a public chat channel as read" }
        }
    }
}
